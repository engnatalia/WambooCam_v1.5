package harmony.valley.wamboocam

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File


class FileUtils {

    // Get a file from a Uri.
    // Framework Documents, as well as the _data field for the MediaStore and
    // other file-based ContentProviders.
    // @param context The context.
    // @param uri     The Uri to query
    @Throws(Exception::class)
    fun getFileFromUri(context: Context, uri: Uri): File {
        var path: String? = null

        // DocumentProvider
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) { // TODO: 2015. 11. 17. KITKAT

                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    val type = split[0]
                    if ("primary".equals(type, ignoreCase = true)) {
                        path =
                            Environment.getExternalStorageDirectory().absolutePath + "/" + split[1]
                    }

                    // TODO handle non-primary volumes
                } else if (isDownloadsDocument(uri)) { // DownloadsProvider
                    val id = DocumentsContract.getDocumentId(uri)
                    val contentUri: Uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        java.lang.Long.valueOf(id)
                    )
                    path = getDataColumn(context, contentUri, null, null)
                } else if (isMediaDocument(uri)) { // MediaProvider
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    val type = split[0]
                    var contentUri: Uri? = null
                    when (type) {
                        "image" -> {
                            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        }
                        "video" -> {
                            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        }
                        "audio" -> {
                            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        }
                    }
                    val selection = "_id=?"
                    val selectionArgs = arrayOf(
                        split[1]
                    )
                    path = getDataColumn(context, contentUri!!, selection, selectionArgs)
                } // MediaStore (and general)
            } else if ("content".equals(uri.scheme, ignoreCase = true)) {
                path = getDataColumn(context, uri, null, null)
            } else if ("file".equals(uri.scheme, ignoreCase = true)) {
                path = uri.path
            }
            File(path)
        } else {
            val cursor: Cursor = context.contentResolver.query(uri, null, null, null, null)!!
            File(cursor.getString(cursor.getColumnIndex("_data")))
        }
    }

    // Get the value of the data column for this Uri. This is useful for
    // MediaStore Uris, and other file-based ContentProviders.
    // @param context       The context.
    // @param uri           The Uri to query.
    // @param selection     (Optional) Filter used in the query.
    // @param selectionArgs (Optional) Selection arguments used in the query.
    // @return The value of the _data column, which is typically a file path.
    private fun getDataColumn(
        context: Context,
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?,
    ): String? {
        var cursor: Cursor? = null
        val column = MediaStore.Images.Media.DATA
        val projection = arrayOf(
            column
        )
        try {
            cursor =
                context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val column_index: Int = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(column_index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }


    // @param uri The Uri to check.
    // @return Whether the Uri authority is ExternalStorageProvide
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    // @param uri The Uri to check.
    // @return Whether the Uri authority is DownloadsProvider.
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    // @param uri The Uri to check.
    // @return Whether the Uri authority is MediaProvider.
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

}