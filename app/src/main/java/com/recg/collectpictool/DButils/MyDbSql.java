package com.recg.collectpictool.DButils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.recg.collectpictool.utils.UploadImgInfo;

import java.util.ArrayList;
import java.util.List;

import cn.finalteam.galleryfinal.utils.ILogger;

/**
 * Created by liupaipai on 2018/7/2.
 */

public class MyDbSql extends SQLiteOpenHelper {
    // 数据库版本号
    private static String dbName = "uploadImg";
    private static Integer Version = 1;
    public static final String CREATE_UPIMG = "create table uploadImg ("+
            "id integer primary key autoincrement, "+
            "grade integer, " +
            "subject text,"+
            "path text)";

    private Context mContext;
    private SQLiteDatabase sqliteDatabase;

    public MyDbSql(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);

        mContext = context;
    }

    public MyDbSql(Context context, String name, int version) {
        this(context, name, null, version);

        mContext = context;
    }

    public MyDbSql(Context context, String name) {
        this(context, name, null, Version);

        mContext = context;
    }

    public MyDbSql(Context context, String name, SQLiteDatabase.CursorFactory factory, int version, DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }


    /**
     19      * 数据库已经创建过了， 则不会执行到，如果不存在数据库则会执行
     20      * @param db
     21      */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_UPIMG); // 执行这句才会创建表
        ILogger.d("SQLite", "create succeeded");
    }

    /**
     31      * 创建数据库时不会执行，增大版本号升级时才会执行到
     32      * @param db
     33      * @param oldVersion
     34      * @param newVersion
     35      */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    private boolean addImg(UploadImgInfo imgInfo){
        sqliteDatabase = getWritableDatabase();
        ContentValues values = new ContentValues();//是用map封装的对象，用来存放值
        values.put("grade", imgInfo.getGrade());
        values.put("subject", imgInfo.getSubject());
        values.put("path", imgInfo.getPath());

        //table: 表名 , nullColumnHack：可以为空，标示添加一个空行, values:数据一行的值 , 返回值：代表添加这个新行的Id ，-1代表添加失败
        long result = sqliteDatabase.insert(dbName,null,values);

        //关闭数据库对象
        sqliteDatabase.close();

        if(result != -1){//-1代表添加失败
            return true;
        }else{
            return false;
        }
    }

    private List<UploadImgInfo> queryImg(){
        List<UploadImgInfo>localUpImgs = new ArrayList<>();
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.rawQuery("select * from uploadImg", null);

        int idIndex = 0;
        int nameIndex = 0;
        int subjectIndext = 0;
        if(cursor.getCount() > 0){
            idIndex = cursor.getColumnIndex("grade");
            nameIndex = cursor.getColumnIndex("path");
            subjectIndext = cursor.getColumnIndex("subject");

            while(cursor.moveToNext()) {
                Integer grade = cursor.getInt(idIndex);
                String path = cursor.getString(nameIndex);
                String subject = cursor.getString(subjectIndext);

                UploadImgInfo uploadImgInfo = new UploadImgInfo(grade,subject,path);
                localUpImgs.add(uploadImgInfo);
            }
            cursor.close();
            return localUpImgs;
        }
        cursor.close();
        return null;
    }
}
