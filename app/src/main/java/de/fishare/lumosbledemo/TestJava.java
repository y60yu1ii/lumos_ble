package de.fishare.lumosbledemo;

import android.content.Context;
import de.fishare.lumosble.CentralManager;
import de.fishare.lumosble.CentralManagerJavaInterface;

public class TestJava {
    public static CentralManager test(Context context){
        return CentralManagerJavaInterface.Companion.getInstance(context);
    }
}
