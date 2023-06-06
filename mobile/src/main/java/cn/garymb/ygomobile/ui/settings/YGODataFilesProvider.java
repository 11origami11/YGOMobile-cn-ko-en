package cn.garymb.ygomobile.ui.settings;

import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Point;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cn.garymb.ygomobile.Constants;
import cn.garymb.ygomobile.lite.R;

public class YGODataFilesProvider extends DocumentsProvider {
    // The default columns to return information about a root if no specific
    // columns are requested in a query.
    private static final String[] DEFAULT_ROOT_PROJECTION = new String[]{
        Root.COLUMN_ROOT_ID,
        Root.COLUMN_MIME_TYPES,
        Root.COLUMN_FLAGS,
        Root.COLUMN_ICON,
        Root.COLUMN_TITLE,
        Root.COLUMN_SUMMARY,
        Root.COLUMN_DOCUMENT_ID,
        Root.COLUMN_AVAILABLE_BYTES
    };

    // The default columns to return information about a document if no specific
    // columns are requested in a query.
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{
        Document.COLUMN_DOCUMENT_ID,
        Document.COLUMN_MIME_TYPE,
        Document.COLUMN_DISPLAY_NAME,
        Document.COLUMN_LAST_MODIFIED,
        Document.COLUMN_FLAGS,
        Document.COLUMN_SIZE
    };

    //数据路径
    private static File DATA_PATH = new File(Constants.PREF_DEF_GAME_DIR);

    @Override
    public boolean onCreate() {
        DATA_PATH = getContext().getExternalFilesDir(Constants.PREF_DEF_GAME_DIR);

        return true;
    }

    // BEGIN_INCLUDE(query_roots) 定义根目录
    @Override
    public Cursor queryRoots(String[] projection) {
        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_ROOT_PROJECTION);
        final String applicationName = getContext().getString(R.string.app_name);

        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Root.COLUMN_ROOT_ID, getDocIdForFile(DATA_PATH));
        row.add(Root.COLUMN_DOCUMENT_ID, getDocIdForFile(DATA_PATH));
        row.add(Root.COLUMN_SUMMARY, null);
        row.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE | Root.FLAG_SUPPORTS_SEARCH | Root.FLAG_SUPPORTS_IS_CHILD);
        row.add(Root.COLUMN_TITLE, applicationName);
        row.add(Root.COLUMN_MIME_TYPES, "*/*");
        row.add(Root.COLUMN_AVAILABLE_BYTES, DATA_PATH.getFreeSpace());
        row.add(Root.COLUMN_ICON, R.drawable.ic_icon);
        return result;
    }

    // BEGIN_INCLUDE(open_document_thumbnail) 显示文档缩略图
    @Override
    public AssetFileDescriptor openDocumentThumbnail(String documentId, Point sizeHint, CancellationSignal signal) throws FileNotFoundException {
        final File file = getFileForDocId(documentId);
        final ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        return new AssetFileDescriptor(pfd, 0, file.length());
    }

    // BEGIN_INCLUDE(query_document) 获取文档信息
    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
        includeFile(result, documentId, null);
        return result;
    }

    // BEGIN_INCLUDE(query_child_documents) 在提供程序中列出文档
    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
        final File parent = getFileForDocId(parentDocumentId);
        for (File file : parent.listFiles()) {
            includeFile(result, null, file);
        }
        return result;
    }

    // BEGIN_INCLUDE(open_document) 打开文档
    @Override
    public ParcelFileDescriptor openDocument(final String documentId, String mode, CancellationSignal signal) throws FileNotFoundException {
        final File file = getFileForDocId(documentId);
        final int accessMode = ParcelFileDescriptor.parseMode(mode);
        return ParcelFileDescriptor.open(file, accessMode);
    }

    // BEGIN_INCLUDE(is_child_document)
    @Override
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        return documentId.startsWith(parentDocumentId);
    }

    // BEGIN_INCLUDE(create_document) 支持文档创建
    @Override
    public String createDocument(String parentDocumentId, String mimeType, String displayName) throws FileNotFoundException {
        File newFile = new File(parentDocumentId, displayName);
        int noConflictId = 2;
        while (newFile.exists()) {
            newFile = new File(parentDocumentId, displayName + " (" + noConflictId++ + ")");
        }
        try {
            boolean succeeded;
            if (Document.MIME_TYPE_DIR.equals(mimeType)) {
                succeeded = newFile.mkdir();
            } else {
                succeeded = newFile.createNewFile();
            }
            if (!succeeded) {
                throw new FileNotFoundException("Failed to create document with id " + newFile.getPath());
            }
        } catch (IOException e) {
            throw new FileNotFoundException("Failed to create document with id " + newFile.getPath());
        }
        return newFile.getPath();
    }

    // BEGIN_INCLUDE(rename_document) 重命名文件
    @Override
    public String renameDocument(String documentId, String displayName) throws FileNotFoundException {
        if (displayName == null) {
            throw new FileNotFoundException("Failed to rename document, new name is null");
        }
        File sourceFile = getFileForDocId(documentId);
        File sourceParentFile = sourceFile.getParentFile();
        if (sourceParentFile == null) {
            throw new FileNotFoundException("Failed to rename document. File has no parent.");
        }
        File destFile = new File(sourceParentFile.getPath(), displayName);
        try {
            boolean renameSucceeded = sourceFile.renameTo(destFile);
            if (!renameSucceeded) {
                throw new FileNotFoundException("Failed to rename document. Renamed failed.");
            }
        } catch (Exception e) {
            throw new FileNotFoundException("Failed to rename document. Error: " + e.getMessage());
        }
        return getDocIdForFile(destFile);
    }

    // BEGIN_INCLUDE(delete_document) 删除文件
    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {
        File file = getFileForDocId(documentId);
        if (!deleteFile(file)) {
            throw new FileNotFoundException("Failed to delete document with id " + documentId);
        }
    }

    // BEGIN_INCLUDE(remove_document) 从某个文件的父目录中将其移除
    @Override
    public void removeDocument(String documentId, String parentDocumentId) throws FileNotFoundException {
        File parent = getFileForDocId(parentDocumentId);
        File file = getFileForDocId(documentId);

        if (file == null) {
            throw new FileNotFoundException("Failed to delete document with id " + documentId);
        }
        boolean doesFileParentMatch = false;
        File fileParent = file.getParentFile();

        if (fileParent == null || fileParent.equals(parent)) {
            doesFileParentMatch = true;
        }
        if (parent.equals(file) || doesFileParentMatch) {
            if (!file.delete()) {
                throw new FileNotFoundException("Failed to delete document with id " + documentId);
            }
        } else {
            throw new FileNotFoundException("Failed to delete document with id " + documentId);
        }
    }

    // BEGIN_INCLUDE(copyDocument) 将文件复制到文档提供程序内的新父目录
    @Override
    public String copyDocument(String sourceDocumentId, String targetParentDocumentId) throws FileNotFoundException {
        File parent = getFileForDocId(targetParentDocumentId);
        File oldFile = getFileForDocId(sourceDocumentId);
        File newFile = new File(parent.getPath(), oldFile.getName());
        try {
            boolean wasNewFileCreated = false;
            if (newFile.createNewFile()) {
                if (newFile.setWritable(true) && newFile.setReadable(true)) {
                    wasNewFileCreated = true;
                }
            }
            if (!wasNewFileCreated) {
                throw new FileNotFoundException("Failed to copy document " + sourceDocumentId +
                        ". Could not create new file.");
            }
            try (InputStream inStream = new FileInputStream(oldFile)) {
                try (OutputStream outStream = new FileOutputStream(newFile)) {
                    // Transfer bytes from in to out
                    byte[] buf = new byte[4096]; // ideal range for network: 2-8k, disk: 8-64k
                    int len;
                    while ((len = inStream.read(buf)) > 0) {
                        outStream.write(buf, 0, len);
                    }
                }
            }
        } catch (IOException e) {
            throw new FileNotFoundException("Failed to copy document: " + sourceDocumentId + ". " + e.getMessage());
        }
        return getDocIdForFile(newFile);
    }

    // BEGIN_INCLUDE(moveDocument) 在文档提供程序内将文件从一个目录移动到另一个目录
    @Override
    public String moveDocument(String sourceDocumentId, String sourceParentDocumentId, String targetParentDocumentId) throws FileNotFoundException {
        try {
            String newDocumentId = copyDocument(sourceDocumentId, sourceParentDocumentId,
                    targetParentDocumentId);
            removeDocument(sourceDocumentId,sourceParentDocumentId);
            return newDocumentId;
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("Failed to move document " + sourceDocumentId);
        }
    }

    @Override
    public String getDocumentType(String documentId) throws FileNotFoundException {
        File file = getFileForDocId(documentId);
        return getMimeType(file);
    }

    /**
     * overload copyDocument to insist that the parent matches
     */
    public String copyDocument(String sourceDocumentId, String sourceParentDocumentId,
                               String targetParentDocumentId) throws FileNotFoundException {
        if (!isChildDocument(sourceParentDocumentId, sourceDocumentId)) {
            throw new FileNotFoundException("Failed to copy document with id " +
                    sourceDocumentId + ". Parent is not: " + sourceParentDocumentId);
        }
        return copyDocument(sourceDocumentId, targetParentDocumentId);
    }

    /**
     * Get the document id given a file. This document id must be consistent across time as other
     * applications may save the ID and use it to reference documents later.
     * <p/>
     * The reverse of @{link #getFileForDocId}.
     */
    private static String getDocIdForFile(File file) {
        return file.getAbsolutePath();
    }

    /**
     * Get the file given a document id (the reverse of {@link #getDocIdForFile(File)}).
     */
    private static File getFileForDocId(String docId) throws FileNotFoundException {
        final File f = new File(docId);
        if (!f.exists()) throw new FileNotFoundException(f.getAbsolutePath() + " not found");
        return f;
    }

    private static String getMimeType(File file) {
        if (file.isDirectory()) {
            return Document.MIME_TYPE_DIR;
        } else {
            final String name = file.getName();
            final int lastDot = name.lastIndexOf('.');
            if (lastDot >= 0) {
                final String extension = name.substring(lastDot + 1).toLowerCase();
                final String mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                if (mime != null) return mime;
            }
            return "application/octet-stream";
        }
    }

    /**
     * Add a representation of a file to a cursor.
     *
     * @param result the cursor to modify
     * @param docId  the document ID representing the desired file (may be null if given file)
     * @param file   the File object representing the desired file (may be null if given docID)
     */
    private void includeFile(MatrixCursor result, String docId, File file)
        throws FileNotFoundException {
        if (docId == null) {
            docId = getDocIdForFile(file);
        } else {
            file = getFileForDocId(docId);
        }

        int flags = 0;
        if (file.isDirectory()) {
            if (file.canWrite()) flags |= Document.FLAG_DIR_SUPPORTS_CREATE;
        } else if (file.canWrite()) {
            flags |= Document.FLAG_SUPPORTS_WRITE;
        }
        if (file.getParentFile().canWrite()) flags |= Document.FLAG_SUPPORTS_DELETE;

        final String displayName = file.getName();
        final String mimeType = getMimeType(file);
        if (mimeType.startsWith("image/")) flags |= Document.FLAG_SUPPORTS_THUMBNAIL;

        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, docId);
        row.add(Document.COLUMN_DISPLAY_NAME, displayName);
        row.add(Document.COLUMN_SIZE, file.length());
        row.add(Document.COLUMN_MIME_TYPE, mimeType);
        row.add(Document.COLUMN_LAST_MODIFIED, file.lastModified());
        row.add(Document.COLUMN_FLAGS, flags);
        row.add(Document.COLUMN_ICON, R.drawable.ic_icon);
    }

    //递归删除文件
    private boolean deleteFile(File file) {
        if (file.isFile()) {
            if (!file.delete()) return false;
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteFile(f);
                }
            }
            if (!file.delete()) return false;
        }
        return true;
    }
}
