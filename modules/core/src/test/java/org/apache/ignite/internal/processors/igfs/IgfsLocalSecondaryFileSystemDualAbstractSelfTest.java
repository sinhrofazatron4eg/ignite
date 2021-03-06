/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.igfs;

import org.apache.ignite.igfs.IgfsFile;
import org.apache.ignite.igfs.IgfsMode;
import org.apache.ignite.igfs.IgfsPath;
import org.apache.ignite.igfs.secondary.IgfsSecondaryFileSystem;
import org.apache.ignite.igfs.secondary.local.LocalIgfsSecondaryFileSystem;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collection;

/**
 * Abstract test for Hadoop 1.0 file system stack.
 */
public abstract class IgfsLocalSecondaryFileSystemDualAbstractSelfTest extends IgfsDualAbstractSelfTest {
    /** */
    private static final String FS_WORK_DIR = U.getIgniteHome() + File.separatorChar + "work"
        + File.separatorChar + "fs";

    /** */
    private static final String FS_EXT_DIR = U.getIgniteHome() + File.separatorChar + "work"
        + File.separatorChar + "ext";

    /** */
    private final File dirLinkDest = new File(FS_EXT_DIR + File.separatorChar + "extdir");

    /** */
    private final File fileLinkDest =
        new File(FS_EXT_DIR + File.separatorChar + "extdir" + File.separatorChar + "filedest");

    /** */
    private final File dirLinkSrc = new File(FS_WORK_DIR + File.separatorChar + "dir");

    /** */
    private final File fileLinkSrc = new File(FS_WORK_DIR + File.separatorChar + "file");


    /** Constructor.
     * @param mode IGFS mode.
     */
    public IgfsLocalSecondaryFileSystemDualAbstractSelfTest(IgfsMode mode) {
        super(mode);
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        final File extDir = new File(FS_EXT_DIR);

        if (!extDir.exists())
            assert extDir.mkdirs();
        else
            cleanDirectory(extDir);
    }


    /**
     * Creates secondary filesystems.
     * @return IgfsSecondaryFileSystem
     * @throws Exception On failure.
     */
    @Override protected IgfsSecondaryFileSystem createSecondaryFileSystemStack() throws Exception {
       final File workDir = new File(FS_WORK_DIR);

        if (!workDir.exists())
            assert workDir.mkdirs();

        LocalIgfsSecondaryFileSystem second = new LocalIgfsSecondaryFileSystem();

        second.setWorkDirectory(workDir.getAbsolutePath());

        igfsSecondary = new IgfsLocalSecondaryFileSystemTestAdapter(workDir);

        return second;
    }

    /** {@inheritDoc} */
    @Override protected boolean permissionsSupported() {
        return false;
    }

    /** {@inheritDoc} */
    @Override protected boolean propertiesSupported() {
        return false;
    }

    /** {@inheritDoc} */
    @Override protected boolean timesSupported() {
        return false;
    }

    /**
     *
     * @throws Exception If failed.
     */
    @SuppressWarnings("ConstantConditions")
    public void testListPathForSymlink() throws Exception {
        if (U.isWindows())
            return;

        createSymlinks();

        assertTrue(igfs.info(DIR).isDirectory());

        Collection<IgfsPath> pathes = igfs.listPaths(DIR);
        Collection<IgfsFile> files = igfs.listFiles(DIR);

        assertEquals(1, pathes.size());
        assertEquals(1, files.size());

        assertEquals("filedest", F.first(pathes).name());
        assertEquals("filedest", F.first(files).path().name());
    }

    /**
     *
     * @throws Exception If failed.
     */
    public void testDeleteSymlinkDir() throws Exception {
        if (U.isWindows())
            return;

        createSymlinks();

        // Only symlink must be deleted. Destination content must be exist.
        igfs.delete(DIR, true);

        assertTrue(fileLinkDest.exists());
    }

    /**
     *
     * @throws Exception If failed.
     */
    public void testSymlinkToFile() throws Exception {
        if (U.isWindows())
            return;

        createSymlinks();

        checkFileContent(igfs, new IgfsPath("/file"), chunk);
    }

    /**
     *
     * @throws Exception If failed.
     */
    private void createSymlinks() throws Exception {
        assert dirLinkDest.mkdir();

        createFile(fileLinkDest, true, chunk);

        Files.createSymbolicLink(dirLinkSrc.toPath(), dirLinkDest.toPath());
        Files.createSymbolicLink(fileLinkSrc.toPath(), fileLinkDest.toPath());
    }

    /**
     * @param dir Directory to clean.
     */
    private static void cleanDirectory(File dir){
        File[] entries = dir.listFiles();

        if (entries != null) {
            for (File entry : entries) {
                if (entry.isDirectory()) {
                    cleanDirectory(entry);

                    assert entry.delete();
                }
                else
                    assert entry.delete();
            }
        }
    }

    /**
     * @param f File object.
     * @param overwrite Overwrite flag.
     * @param chunks File content.
     * @throws IOException If failed.
     */
    private static void createFile(File f, boolean overwrite, @Nullable byte[]... chunks) throws IOException {
        OutputStream os = null;

        try {
            os = new FileOutputStream(f, overwrite);

            writeFileChunks(os, chunks);
        }
        finally {
            U.closeQuiet(os);
        }
    }
}