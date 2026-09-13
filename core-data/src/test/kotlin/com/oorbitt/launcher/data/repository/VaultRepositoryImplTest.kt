package com.oorbitt.launcher.data.repository

import android.util.Base64
import com.google.common.truth.Truth.assertThat
import com.oorbitt.launcher.data.db.dao.VaultMemoDao
import com.oorbitt.launcher.data.db.entity.VaultMemoEntity
import com.oorbitt.launcher.data.repository.impl.VaultRepositoryImpl
import com.oorbitt.launcher.model.MemoBlock
import com.oorbitt.launcher.model.VaultMemo
import com.oorbitt.launcher.security.EncryptionEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class VaultRepositoryImplTest {

    private val dao: VaultMemoDao = mockk(relaxed = true)
    private val encryptionEngine: EncryptionEngine = mockk()
    private lateinit var repository: VaultRepository

    @Before
    fun setUp() {
        mockkStatic(Base64::class)
        // Mock android.util.Base64 using java.util.Base64
        every { Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg())
        }
        every { Base64.decode(any<String>(), any()) } answers {
            java.util.Base64.getDecoder().decode(firstArg<String>())
        }

        // Simple pass-through mocks for encryption engine
        every { encryptionEngine.encrypt(any()) } answers { firstArg() }
        every { encryptionEngine.decrypt(any()) } answers { firstArg() }

        repository = VaultRepositoryImpl(dao, encryptionEngine)
    }

    @Test
    fun getAllMemos_returnsDecryptedMemos() = runTest {
        val entity = VaultMemoEntity(
            id = 1L,
            title = java.util.Base64.getEncoder().encodeToString("Secret Title".toByteArray()),
            createdAt = 12345L,
            updatedAt = 67890L,
            isPinned = true,
            color = "RED",
            blocksJson = java.util.Base64.getEncoder().encodeToString(
                """[{"id":"b1","type":"text","markdown":"My confidential text"}]""".toByteArray()
            )
        )

        every { dao.getAll() } returns flowOf(listOf(entity))

        val memos = repository.getAllMemos().first()
        
        assertThat(memos).hasSize(1)
        val memo = memos.first()
        assertThat(memo.id).isEqualTo(1L)
        assertThat(memo.title).isEqualTo("Secret Title")
        assertThat(memo.isPinned).isTrue()
        assertThat(memo.blocks).hasSize(1)
        
        val block = memo.blocks.first() as MemoBlock.Text
        assertThat(block.id).isEqualTo("b1")
        assertThat(block.markdown).isEqualTo("My confidential text")
    }

    @Test
    fun saveMemo_encryptsTitleAndBlocks_thenInsertsToDao() = runTest {
        val memo = VaultMemo(
            id = 0L,
            title = "Personal Diary",
            isPinned = false,
            blocks = listOf(MemoBlock.Text(id = "b2", markdown = "Dear diary..."))
        )

        coEvery { dao.insert(any()) } returns 42L

        val id = repository.saveMemo(memo)
        assertThat(id).isEqualTo(42L)

        coVerify {
            dao.insert(match { entity ->
                val decryptedTitle = String(java.util.Base64.getDecoder().decode(entity.title))
                decryptedTitle == "Personal Diary"
            })
        }
    }
}
