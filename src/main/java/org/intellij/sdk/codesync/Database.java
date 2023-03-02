package org.intellij.sdk.codesync;

import org.bouncycastle.jcajce.provider.digest.MD2;

import java.util.ArrayList;
import java.util.HashMap;

public class Database {

    public static db instance;

    public static void initiate() {

        instance = new db();
        instance.connect();
    }

    public static ArrayList runQuery(String query){
        return instance.executeQuery(query);
    }

    public static void executeUpdate(String query){
        instance.executeUpdate(query);
    }

    public static void disconnect(){
        instance.disconnect();
    }

}
