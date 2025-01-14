package jp.panta.misskeyandroidclient.model.drive

import junit.framework.TestCase
import net.pantasystem.milktea.model.drive.*

class DriveStoreTest : TestCase() {

    fun testToggleSelect() {

        val driveStore = DriveStore(
            DriveState(
                accountId = 0L,
                selectedFilePropertyIds = SelectedFilePropertyIds(4, emptySet()),
                path = DirectoryPath(emptyList())
            )
        )
        driveStore.toggleSelect(FileProperty.Id(0L, "fileA"))
        assertEquals(setOf(FileProperty.Id(0L, "fileA")), driveStore.state.value.selectedFilePropertyIds?.selectedIds)
        driveStore.toggleSelect(FileProperty.Id(0L, "fileB"))
        driveStore.toggleSelect(FileProperty.Id(0L, "fileC"))
        driveStore.toggleSelect(FileProperty.Id(0L, "fileD"))
        assertEquals(
            setOf(
                FileProperty.Id(0L, "fileA"),
                FileProperty.Id(0L, "fileB"),
                FileProperty.Id(0L, "fileC"),
                FileProperty.Id(0L, "fileD")
            ),
            driveStore.state.value.selectedFilePropertyIds?.selectedIds
        )

        driveStore.toggleSelect(FileProperty.Id(0L, "fileE"))

        // NOTE: 最大サイズを超えて追加できないことを確認している
        assertEquals(
            setOf(
                FileProperty.Id(0L, "fileA"),
                FileProperty.Id(0L, "fileB"),
                FileProperty.Id(0L, "fileC"),
                FileProperty.Id(0L, "fileD")
            ),
            driveStore.state.value.selectedFilePropertyIds?.selectedIds
        )

        driveStore.toggleSelect(FileProperty.Id(0L, "fileD"))
        assertEquals(
            setOf(
                FileProperty.Id(0L, "fileA"),
                FileProperty.Id(0L, "fileB"),
                FileProperty.Id(0L, "fileC"),
            ),
            driveStore.state.value.selectedFilePropertyIds?.selectedIds
        )
    }

    fun testSelect() {
        val driveStore = DriveStore(
            DriveState(
                accountId = 0L,
                selectedFilePropertyIds = SelectedFilePropertyIds(4, emptySet()),
                path = DirectoryPath(emptyList())
            )
        )
        driveStore.select(FileProperty.Id(0L, "fileA"))
        assertEquals(setOf(FileProperty.Id(0L, "fileA")), driveStore.state.value.selectedFilePropertyIds?.selectedIds)

    }

    fun testDeselect() {
        val driveStore = DriveStore(
            DriveState(
                accountId = 0L,
                selectedFilePropertyIds = SelectedFilePropertyIds(
                    4,
                    setOf(
                        FileProperty.Id(0L, "fileA"),
                        FileProperty.Id(0L, "fileB"),
                        FileProperty.Id(0L, "fileC"),
                        FileProperty.Id(0L, "fileD")
                    )
                ),
                path = DirectoryPath(emptyList())
            )
        )
        val fileIds = listOf("fileA", "fileB", "fileC", "fileD")
        fileIds.forEach {
            driveStore.deselect(FileProperty.Id(0L, it))
        }
        assertTrue(
            driveStore.state.value.selectedFilePropertyIds?.selectedIds.isNullOrEmpty()
        )
    }

    fun testPop() {
        val directories = listOf("dir1", "dir2", "dir3", "dir4", "di5")
        val root = Directory(
            "root",
            "",
            "",
            0,
            0,
            null,
            null
        )
        val driveStore = DriveStore(
            DriveState(
                accountId = 0L,
                selectedFilePropertyIds = null,
                path = DirectoryPath(listOf(root))
            )
        )
        directories.forEach {
            val parent = driveStore.state.value.path.path.last()
            driveStore.push(
                root.copy(
                    parent = parent,
                    parentId = parent.id,
                    id = it,
                    name = "${it}name"
                )
            )
        }
        directories.reversed().forEach {
            assertEquals(it, driveStore.state.value.path.path.last().id)
            assertTrue(driveStore.pop())
        }

    }


}