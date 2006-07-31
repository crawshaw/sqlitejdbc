#include <stdlib.h>
#include <string.h>
#include "DB.h"
#include "sqlite3.h"

static jclass    CLS_DB           = 0;
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

static void throwexmsg(JNIEnv *env, jobject this, const char *str)
{
    (*env)->CallVoidMethod(env, this, MTH_throwexmsg,
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

static jsize jstrlen(const jchar *str)
{
    const jchar *s;
    int suppChars = 0;

    // any UTF-16 character that is greater than \xD800 is the first
    // of a supplementary character, represented by a pair of 16-bit
    // blocks. So we ignore one of the two (the second must always
    // be less than \xD800 according to the standard).
    for (s = str; *s; s++)
        if (*s > 0xD800) suppChars++;

    return (int)(s - str) - suppChars;
}


JNIEXPORT void JNICALL Java_org_sqlite_DB_init(JNIEnv *env, jclass cls)
{
    CLS_DB = (*env)->FindClass(env, "org/sqlite/DB");
    JNI_DB_pointer = (*env)->GetFieldID( env, CLS_DB, "pointer", "J");
    MTH_throwex    = (*env)->GetMethodID(env, CLS_DB, "throwex", "()V");
    MTH_throwexmsg = (*env)->GetMethodID(env, CLS_DB, "throwex",
        "(Ljava/lang/String;)V");
}

JNIEXPORT void JNICALL Java_org_sqlite_DB_open(
        JNIEnv *env, jobject this, jstring file)
{
    int ret;
    sqlite3 *db = gethandle(env, this);
    const char *str;

    if (db) {
        throwexmsg(env, this, "DB already open");
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
    sqlite3_close(gethandle(env, this)); // TODO: error checking
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

JNIEXPORT jint JNICALL Java_org_sqlite_DB_bind_1null(
        JNIEnv *env, jobject this, jlong stmt, jint pos)
{
    return sqlite3_bind_null(toref(stmt), pos);
}

JNIEXPORT jint JNICALL Java_org_sqlite_DB_bind_1text(
        JNIEnv *env, jobject this, jlong stmt, jint pos, jstring value)
{
    const jchar *str =(*env)->GetStringCritical(env, value, 0);
    jint ret;

    if (str == NULL) exit(1);
    ret = sqlite3_bind_text16(toref(stmt), pos, str, -1, SQLITE_TRANSIENT);
    (*env)->ReleaseStringCritical(env, value, str);
    return ret;
}

JNIEXPORT jint JNICALL Java_org_sqlite_DB_bind_1blob(
        JNIEnv *env, jobject this, jlong stmt, jint pos, jbyteArray value)
{
    if (value == NULL) return sqlite3_bind_null(toref(stmt), pos);
    jint length, ret;
    jbyte *a;

    length = (*env)->GetArrayLength(env, value); 
    a = (*env)->GetPrimitiveArrayCritical(env, value, 0);
    if (a == NULL) exit(1);
    ret = sqlite3_bind_blob(toref(stmt), pos, a, length, SQLITE_TRANSIENT);
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
    const void *str = sqlite3_column_text16(toref(stmt), col);
    return str ? (*env)->NewString(env, str, jstrlen(str)) : NULL;
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
    if (jBlob == NULL) exit(1);
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
            throwexmsg(env, this, "query returns results"); break;
        case SQLITE_BUSY:
            throwexmsg(env, this, "database locked"); break;
        case SQLITE_MISUSE:
            throwexmsg(env, this, "JDBC internal consistency error"); break;
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
    const char *zDbName = 0; // FIXME ?
    const char *zTableName;
    const char *zColumnName;

    char const *pzDataType;   /* OUTPUT: Declared data type */
    char const *pzCollSeq;    /* OUTPUT: Collation sequence name */
    int pNotNull;             /* OUTPUT: True if NOT NULL constraint exists */
    int pPrimaryKey;          /* OUTPUT: True if column part of PK */
    int pAutoinc;             /* OUTPUT: True if colums is auto-increment */

    int i;
    int length = (*env)->GetArrayLength(env, colNames);
    jobjectArray array = (*env)->NewObjectArray(
        env, length, (*env)->FindClass(env, "[Z"), NULL) ;

    jboolean* colDataRaw = (jboolean*)malloc(3 * sizeof(jboolean));
    jstring colName;
    jbooleanArray colData;

    for (i = 0; i < length; i++) {
        // load passed column name and table name
        colName     = (jstring) (*env)->GetObjectArrayElement(env, colNames, i);
        zColumnName = (*env)->GetStringUTFChars(env, colName, 0);
        zTableName  = sqlite3_column_table_name(toref(stmt), i);

        // request metadata for column and load into output variables
        sqlite3_table_column_metadata(
            gethandle(env, this), zDbName, zTableName, zColumnName,
            &pzDataType, &pzCollSeq, &pNotNull, &pPrimaryKey, &pAutoinc
        );

        (*env)->ReleaseStringUTFChars(env, colName, zColumnName);

        // load relevant metadata into 2nd dimension of return results
        colDataRaw[0] = pNotNull;
        colDataRaw[1] = pPrimaryKey;
        colDataRaw[2] = pAutoinc;

        colData = (*env)->NewBooleanArray(env, 3);
        (*env)->SetBooleanArrayRegion(env, colData, 0, 3, colDataRaw);
        (*env)->SetObjectArrayElement(env, array, i, colData);
    }

    free(colDataRaw);

    return array;
}

