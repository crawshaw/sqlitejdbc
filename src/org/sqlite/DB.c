/* Copyright 2006 David Crawshaw, see LICENSE file for licensing [BSD]. */
#include <stdlib.h>
#include <string.h>
#include "DB.h"
#include "sqlite3.h"

static jclass    dbclass          = 0;
static jfieldID  JNI_DB_pointer   = 0;
static jmethodID MTH_throwex      = 0;
static jmethodID MTH_throwexmsg   = 0;


static void * toref(jlong value)
{
    jvalue ret;
    ret.j = value;
    return (void *) ret.l;
}

static jlong fromref(void * value)
{
    jvalue ret;
    ret.l = value;
    return ret.j;
}

static void throwex(JNIEnv *env, jobject this)
{
    (*env)->CallVoidMethod(env, this, MTH_throwex);
}

static void throwexmsg(JNIEnv *env, const char *str)
{
    (*env)->CallStaticVoidMethod(env, dbclass, MTH_throwexmsg,
                                (*env)->NewStringUTF(env, str));
}

static sqlite3 * gethandle(JNIEnv *env, jobject this)
{
    return (sqlite3 *)toref((*env)->GetLongField(env, this, JNI_DB_pointer));
}

static void sethandle(JNIEnv *env, jobject this, sqlite3 * ref)
{
    (*env)->SetLongField(env, this, JNI_DB_pointer, fromref(ref));
}

/* Returns number of 16-bit blocks in UTF-16 string, not including null. */
static jsize jstrlen(const jchar *str)
{
    const jchar *s;
    for (s = str; *s; s++);
    return (jsize)(s - str);
}


JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JNIEnv* env = 0;

    if (JNI_OK != (*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_2))
        return JNI_ERR;

    dbclass = (*env)->NewGlobalRef(env,
        (*env)->FindClass(env, "org/sqlite/DB"));
    if (!dbclass) return JNI_ERR;

    JNI_DB_pointer = (*env)->GetFieldID( env, dbclass, "pointer", "J");
    MTH_throwex    = (*env)->GetMethodID(env, dbclass, "throwex", "()V");
    MTH_throwexmsg = (*env)->GetStaticMethodID(env, dbclass, "throwex",
        "(Ljava/lang/String;)V");

    if (!JNI_DB_pointer || !MTH_throwex || !MTH_throwexmsg) return JNI_ERR;


    return JNI_VERSION_1_2;
}

JNIEXPORT void JNICALL Java_org_sqlite_DB_open(
        JNIEnv *env, jobject this, jstring file)
{
    int ret;
    sqlite3 *db = gethandle(env, this);
    const char *str;

    if (db) {
        throwexmsg(env, "DB already open");
        sqlite3_close(db);
        return;
    }

    str = (*env)->GetStringUTFChars(env, file, 0); 
    if (sqlite3_open(str, &db)) {
        throwex(env, this);
        sqlite3_close(db);
        return;
    }
    (*env)->ReleaseStringUTFChars(env, file, str);

    sethandle(env, this, db);
}

JNIEXPORT void JNICALL Java_org_sqlite_DB_close(JNIEnv *env, jobject this)
{
    if (sqlite3_close(gethandle(env, this)) != SQLITE_OK)
        throwex(env, this);
    sethandle(env, this, 0);
}

JNIEXPORT void JNICALL Java_org_sqlite_DB_interrupt(JNIEnv *env, jobject this)
{
    sqlite3_interrupt(gethandle(env, this));
}

JNIEXPORT void JNICALL Java_org_sqlite_DB_busy_1timeout(
    JNIEnv *env, jobject this, jint ms)
{
    sqlite3_busy_timeout(gethandle(env, this), ms);
}

JNIEXPORT void JNICALL Java_org_sqlite_DB_exec(
        JNIEnv *env, jobject this, jstring sql)
{
    const char *strsql = (*env)->GetStringUTFChars(env, sql, 0);
    if (sqlite3_exec(gethandle(env, this), strsql, 0, 0, 0) != SQLITE_OK)
        throwex(env, this);
    (*env)->ReleaseStringUTFChars(env, sql, strsql);
}

JNIEXPORT jlong JNICALL Java_org_sqlite_DB_prepare(
        JNIEnv *env, jobject this, jstring sql)
{
    sqlite3* db = gethandle(env, this);
    sqlite3_stmt* stmt;

    const char *strsql = (*env)->GetStringUTFChars(env, sql, 0);
    int status = sqlite3_prepare(db, strsql, -1, &stmt, 0);
    (*env)->ReleaseStringUTFChars(env, sql, strsql);

    if (status != SQLITE_OK) {
        throwex(env, this);
        return fromref(0);
    }
    return fromref(stmt);
}

JNIEXPORT jstring JNICALL Java_org_sqlite_DB_errmsg(JNIEnv *env, jobject this)
{
    return (*env)->NewStringUTF(env, sqlite3_errmsg(gethandle(env, this)));
}

JNIEXPORT jstring JNICALL Java_org_sqlite_DB_libversion(
        JNIEnv *env, jobject this)
{
    return (*env)->NewStringUTF(env, sqlite3_libversion());
}

JNIEXPORT jint JNICALL Java_org_sqlite_DB_changes(
        JNIEnv *env, jobject this, jlong stmt)
{
    return sqlite3_changes(gethandle(env, this));
}

JNIEXPORT jint JNICALL Java_org_sqlite_DB_finalize(
        JNIEnv *env, jobject this, jlong stmt)
{
    return sqlite3_finalize(toref(stmt));
}

JNIEXPORT jint JNICALL Java_org_sqlite_DB_step(
        JNIEnv *env, jobject this, jlong stmt)
{
    return sqlite3_step(toref(stmt));
}

JNIEXPORT jint JNICALL Java_org_sqlite_DB_reset(
        JNIEnv *env, jobject this, jlong stmt)
{
    return sqlite3_reset(toref(stmt));
}

JNIEXPORT jint JNICALL Java_org_sqlite_DB_clear_1bindings(
        JNIEnv *env, jobject this, jlong stmt)
{
    int i;
    int count = sqlite3_bind_parameter_count(toref(stmt));
    jint rc = SQLITE_OK;
    for(i=1; rc==SQLITE_OK && i <= count; i++) {
        rc = sqlite3_bind_null(toref(stmt), i);
    }
    return rc;
}

JNIEXPORT jint JNICALL Java_org_sqlite_DB_bind_1parameter_1count(
        JNIEnv *env, jobject this, jlong stmt)
{
    return sqlite3_bind_parameter_count(toref(stmt));
}

JNIEXPORT jint JNICALL Java_org_sqlite_DB_bind_1null(
        JNIEnv *env, jobject this, jlong stmt, jint pos)
{
    return sqlite3_bind_null(toref(stmt), pos);
}

JNIEXPORT jint JNICALL Java_org_sqlite_DB_bind_1text(
        JNIEnv *env, jobject this, jlong stmt, jint pos, jstring value)
{
    const jchar *str;
    jint ret;
    jsize size;

    if (str == NULL) return sqlite3_bind_null(toref(stmt), pos);
    size = (*env)->GetStringLength(env, value) * 2; // in bytes

    // be careful with the *Critical functions, they turn off the GC
    str = (*env)->GetStringCritical(env, value, 0);
    if (str == NULL) exit(1); // out-of-memory
    ret = sqlite3_bind_text16(toref(stmt), pos, str, size, SQLITE_TRANSIENT);
    (*env)->ReleaseStringCritical(env, value, str);

    return ret;
}

JNIEXPORT jint JNICALL Java_org_sqlite_DB_bind_1blob(
        JNIEnv *env, jobject this, jlong stmt, jint pos, jbyteArray value)
{
    jbyte *a;
    jint ret;
    jsize size;

    if (value == NULL) return sqlite3_bind_null(toref(stmt), pos);
    size = (*env)->GetArrayLength(env, value);

    // be careful with *Critical
    a = (*env)->GetPrimitiveArrayCritical(env, value, 0);
    if (a == NULL) exit(1); // out-of-memory
    ret = sqlite3_bind_blob(toref(stmt), pos, a, size, SQLITE_TRANSIENT);
    (*env)->ReleasePrimitiveArrayCritical(env, value, a, JNI_ABORT);
    return ret;
}

JNIEXPORT jint JNICALL Java_org_sqlite_DB_bind_1double(
        JNIEnv *env, jobject this, jlong stmt, jint pos, jdouble value)
{
    return sqlite3_bind_double(toref(stmt), pos, value);
}

JNIEXPORT jint JNICALL Java_org_sqlite_DB_bind_1long(
        JNIEnv *env, jobject this, jlong stmt, jint pos, jlong value)
{
    return sqlite3_bind_int64(toref(stmt), pos, value);
}

JNIEXPORT jint JNICALL Java_org_sqlite_DB_bind_1int(
        JNIEnv *env, jobject this, jlong stmt, jint pos, jint value)
{
    return sqlite3_bind_int(toref(stmt), pos, value);
}

JNIEXPORT jint JNICALL Java_org_sqlite_DB_column_1count(
        JNIEnv *env, jobject this, jlong stmt)
{
    return sqlite3_column_count(toref(stmt));
}

JNIEXPORT jint JNICALL Java_org_sqlite_DB_column_1type(
        JNIEnv *env, jobject this, jlong stmt, jint col)
{
    return sqlite3_column_type(toref(stmt), col);
}

JNIEXPORT jstring JNICALL Java_org_sqlite_DB_column_1decltype(
        JNIEnv *env, jobject this, jlong stmt, jint col)
{
    const char *str = sqlite3_column_decltype(toref(stmt), col);
    return (*env)->NewStringUTF(env, str);
}

JNIEXPORT jstring JNICALL Java_org_sqlite_DB_column_1table_1name(
        JNIEnv *env, jobject this, jlong stmt, jint col)
{
    const void *str = sqlite3_column_table_name16(toref(stmt), col);
    return str ? (*env)->NewString(env, str, jstrlen(str)) : NULL;
}

JNIEXPORT jstring JNICALL Java_org_sqlite_DB_column_1name(
        JNIEnv *env, jobject this, jlong stmt, jint col)
{
    const void *str = sqlite3_column_name16(toref(stmt), col);
    return str ? (*env)->NewString(env, str, jstrlen(str)) : NULL;
}

JNIEXPORT jstring JNICALL Java_org_sqlite_DB_column_1text(
        JNIEnv *env, jobject this, jlong stmt, jint col)
{
    jint length = sqlite3_column_bytes16(toref(stmt), col) / 2; // in jchars
    const void *str = sqlite3_column_text16(toref(stmt), col);
    return str ? (*env)->NewString(env, str, length) : NULL;
}

JNIEXPORT jbyteArray JNICALL Java_org_sqlite_DB_column_1blob(
        JNIEnv *env, jobject this, jlong stmt, jint col)
{
    jsize length;
    jbyteArray jBlob;
    jbyte *a;
    const void *blob = sqlite3_column_blob(toref(stmt), col);
    if (!blob) return NULL;

    length = sqlite3_column_bytes(toref(stmt), col);
    jBlob = (*env)->NewByteArray(env, length);
    if (jBlob == NULL) exit(1); // out-of-memory

    a = (*env)->GetPrimitiveArrayCritical(env, jBlob, 0);
    memcpy(a, blob, length);
    (*env)->ReleasePrimitiveArrayCritical(env, jBlob, a, 0);

    return jBlob;
}

JNIEXPORT jdouble JNICALL Java_org_sqlite_DB_column_1double(
        JNIEnv *env, jobject this, jlong stmt, jint col)
{
    return sqlite3_column_double(toref(stmt), col);
}

JNIEXPORT jlong JNICALL Java_org_sqlite_DB_column_1long(
        JNIEnv *env, jobject this, jlong stmt, jint col)
{
    return sqlite3_column_int64(toref(stmt), col);
}

JNIEXPORT jint JNICALL Java_org_sqlite_DB_column_1int(
        JNIEnv *env, jobject this, jlong stmt, jint col)
{
    return sqlite3_column_int(toref(stmt), col);
}


// COMPOUND FUNCTIONS ///////////////////////////////////////////////

JNIEXPORT jint JNICALL Java_org_sqlite_DB_executeUpdate(
        JNIEnv *env, jobject this, jlong stmt)
{
    sqlite3_stmt *dbstmt = toref(stmt);
    jint changes = 0;

    switch (sqlite3_step(dbstmt)) {
        case SQLITE_DONE:
            changes = sqlite3_changes(gethandle(env, this)); break;
        case SQLITE_ROW:
            throwexmsg(env, "query returns results"); break;
        case SQLITE_BUSY:
            throwexmsg(env, "database locked"); break;
        case SQLITE_MISUSE:
            throwexmsg(env, "JDBC internal consistency error"); break;
        case SQLITE_ERROR:
        default:
            throwex(env, this); return 0;
    }

    sqlite3_reset(dbstmt);
    return changes;
}

JNIEXPORT jobjectArray JNICALL Java_org_sqlite_DB_column_1names(
        JNIEnv *env, jobject this, jlong stmt)
{
    int i;
    const void *str;
    sqlite3_stmt *dbstmt = toref(stmt);
    int col_count = sqlite3_column_count(dbstmt);

    jobjectArray names = (*env)->NewObjectArray(
            env,
            col_count,
            (*env)->FindClass(env, "java/lang/String"),
            (*env)->NewStringUTF(env, "")
    );

    for (i=0; i < col_count; i++) {
        str = sqlite3_column_name16(dbstmt, i);
        (*env)->SetObjectArrayElement(
            env, names, i,
            str ? (*env)->NewString(env, str, jstrlen(str)) : NULL);
    }

    return names;
}

JNIEXPORT jobjectArray JNICALL Java_org_sqlite_DB_column_1metadata(
        JNIEnv *env, jobject this, jlong stmt, jobjectArray colNames)
{
    const char *zTableName;
    const char *zColumnName;
    int pNotNull, pPrimaryKey, pAutoinc;

    int i, length;
    jobjectArray array;

    jstring colName;
    jbooleanArray colData;
    jboolean* colDataRaw;

    sqlite3 *db;
    sqlite3_stmt *dbstmt;


    db = gethandle(env, this);
    dbstmt = toref(stmt);

    length = (*env)->GetArrayLength(env, colNames);

    array = (*env)->NewObjectArray(
        env, length, (*env)->FindClass(env, "[Z"), NULL) ;
    if (array == NULL) exit(1); // out-of-memory

    colDataRaw = (jboolean*)malloc(3 * sizeof(jboolean));
    if (colDataRaw == NULL) exit(1); // out-of-memory


    for (i = 0; i < length; i++) {
        // load passed column name and table name
        colName     = (jstring)(*env)->GetObjectArrayElement(env, colNames, i);
        zColumnName = (*env)->GetStringUTFChars(env, colName, 0);
        zTableName  = sqlite3_column_table_name(dbstmt, i);

        // request metadata for column and load into output variables
        sqlite3_table_column_metadata(
            db, 0, zTableName, zColumnName,
            0, 0, &pNotNull, &pPrimaryKey, &pAutoinc
        );

        (*env)->ReleaseStringUTFChars(env, colName, zColumnName);

        // load relevant metadata into 2nd dimension of return results
        colDataRaw[0] = pNotNull;
        colDataRaw[1] = pPrimaryKey;
        colDataRaw[2] = pAutoinc;

        colData = (*env)->NewBooleanArray(env, 3);
        if (colData == NULL) exit(1); // out-of-memory

        (*env)->SetBooleanArrayRegion(env, colData, 0, 3, colDataRaw);
        (*env)->SetObjectArrayElement(env, array, i, colData);
    }

    free(colDataRaw);

    return array;
}

