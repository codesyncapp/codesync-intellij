package org.intellij.sdk.codesync

class Helper {



    companion object {
        val DIRECTORY_PATH = System.getProperty("user.dir") + "\\src\\test\\java\\test_data"
        val DATABASE_FILE = "\\file.db"
        val CONNECTION_STRING = "jdbc:sqlite:" + DIRECTORY_PATH + DATABASE_FILE
    }

}