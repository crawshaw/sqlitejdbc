/* Copyright 2006 David Crawshaw, see LICENSE file for licensing [BSD]. */
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include "DB.h"
#include "sqlite3.h"

static jclass dbclass = 0;
static jclass  fclass = 0;
static jclass  aclass = 0;

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
    static jmethodID mth_throwex = 0;

    if (!mth_throwex)
        mth_throwex = (*env)->GetMethodID(env, dbclass, "throwex", "()V");

    (*env)->CallVoidMethod(env, this, mth_throwex);
}

static void throwexmsg(JNIEnv *env, const char *str)
{
    static jmethodID mth_throwexmsg = 0;

    if (!mth_throwexmsg) mth_throwexmsg = (*env)->GetStaticMethodID(
            env, dbclass, "throwex", "(Ljava/lang/String;)V");

    (*env)->CallStaticVoidMethod(env, dbclass, mth_throwexmsg,
                                (*env)->NewStringUTF(env, str));
}

static sqlite3 * gethandle(JNIEnv *env, jobject this)
{
    static jfieldID pointer = 0;
    if (!pointer) pointer = (*env)->GetFieldID(env, dbclass, "pointer", "J");

    return (sqlite3 *)toref((*env)->GetLongField(env, this, pointer));
}

static void sethandle(JNIEnv *env, jobject this, sqlite3 * ref)
{
    static jfieldID pointer = 0;
    if (!pointer) pointer = (*env)->GetFieldID(env, dbclass, "pointer", "J");

    (*env)->SetLongField(env, this, pointer, fromref(ref));
}

/* Returns number of 16-bit blocks in UTF-16 string, not including null. */
static jsize jstrlen(const jchar *str)
{
    const jchar *s;
    for (s = str; *s; s++);
    return (jsize)(s - str);
}


// User Defined Function SUPPORT ////////////////////////////////////

typedef struct UDFData UDFData;
struct UDFData {
    JNIEnv *env;
    jobject func;
    UDFData *next;  // linked list of all UDFData instances
};

/* Returns the sqlite3_value for the given arg of the given function.
 * If 0 is returned, an exception has been thrown to report the reason. */
static sqlite3_value * tovalue(JNIEnv *env, jobject function, jint arg)
{
    jlong value_pntr = 0;
    jint numArgs = 0;
    static jfieldID func_value = 0,
                    func_args = 0;

    if (!func_value || !func_args) {
        func_value = (*env)->GetFieldID(env, fclass, "value", "J");
        func_args  = (*env)->GetFieldID(env, fclass, "args", "I");
    }

    // check we have any business being here
    if (arg  < 0) { throwexmsg(env, "negative arg out of range"); return 0; }
    if (!function) { throwexmsg(env, "inconstent function"); return 0; }

    value_pntr = (*env)->GetLongField(env, function, func_value);
    numArgs = (*env)->GetIntField(env, function, func_args);

    if (value_pntr == 0) { throwexmsg(env, "no current value"); return 0; }
    if (arg >= numArgs) { throwexmsg(env, "arg out of range"); return 0; }

    return ((sqlite3_value**)toref(value_pntr))[arg];
}

/* called if an exception occured processing xFunc */
static void xFunc_error(sqlite3_context *context, JNIEnv *env)
{
    const char *strmsg = 0;
    jstring msg = 0;
    jint msgsize = 0;

    jclass exclass = 0;
    static jmethodID exp_msg = 0;
    jthrowable ex = (*env)->ExceptionOccurred(env);

    (*env)->ExceptionClear(env);

    if (!exp_msg) {
        exclass = (*env)->FindClass(env, "java/lang/Throwable");
        exp_msg = (*env)->GetMethodID(
                env, exclass, "toString", "()Ljava/lang/String;");
    }

    msg = (jstring)(*env)->CallObjectMethod(env, ex, exp_msg);
    if (!msg) { sqlite3_result_error(context, "unknown error", 13); return; }

    msgsize = (*env)->GetStringUTFLength(env, msg);
    strmsg = (*env)->GetStringUTFChars(env, msg, 0);
    assert(strmsg); // out-of-memory

    sqlite3_result_error(context, strmsg, msgsize);

    (*env)->ReleaseStringUTFChars(env, msg, strmsg);
}

/* used to call xFunc, xStep and xFinal */
static xCall(
    sqlite3_context *context,
    int args,
    sqlite3_value** value,
    jobject func,
    jmethodID method)
{
    static jfieldID fld_context = 0,
                     fld_value = 0,
                     fld_args = 0;
    JNIEnv *env = 0;
    UDFData *udf = 0;

    udf = (UDFData*)sqlite3_user_data(context);
    env = udf->env;
    if (!func) func = udf->func;

    if (!fld_context || !fld_value || !fld_args) {
        fld_context = (*env)->GetFieldID(env, fclass, "context", "J");
        fld_value   = (*env)->GetFieldID(env, fclass, "value", "J");
        fld_args    = (*env)->GetFieldID(env, fclass, "args", "I");
    }

    (*env)->SetLongField(env, func, fld_context, fromref(context));
    (*env)->SetLongField(env, func, fld_value, value ? fromref(value) : 0);
    (*env)->SetLongField(env, func, fld_args, args);

    (*env)->CallVoidMethod(env, func, method);

    (*env)->SetLongField(env, func, fld_context, 0);
    (*env)->SetLongField(env, func, fld_value, 0);
    (*env)->SetLongField(env, func, fld_args, 0);

    // check if xFunc threw an Exception
    if ((*env)->ExceptionCheck(env)) xFunc_error(context, env);
}


void xFunc(sqlite3_context *context, int args, sqlite3_value** value)
{
    static jmethodID mth = 0;
    if (!mth) {
        JNIEnv *env = ((UDFData*)sqlite3_user_data(context))->env;
        mth = (*env)->GetMethodID(env, fclass, "xFunc", "()V");
    }
    xCall(context, args, value, 0, mth);
}

void xStep(sqlite3_context *context, int args, sqlite3_value** value)
{
    jobject *func = 0;
    static jmethodID mth = 0;
    static jmethodID clone = 0;

    if (!mth || !clone) {
        JNIEnv *env = ((UDFData*)sqlite3_user_data(context))->env;
        mth = (*env)->GetMethodID(env, aclass, "xStep", "()V");
        clone = (*env)->GetMethodID(env, aclass, "clone",
            "()Ljava/lang/Object;");
    }

    // clone the Function.Aggregate instance and store a pointer
    // in SQLite's aggregate_context (clean up in xFinal)
    func = sqlite3_aggregate_context(context, sizeof(jobject));
    if (!*func) {
        UDFData *udf = (UDFData*)sqlite3_user_data(context);
        *func = (*udf->env)->CallObjectMethod(udf->env, udf->func, clone);
        *func = (*udf->env)->NewGlobalRef(udf->env, *func);
    }

    xCall(context, args, value, *func, mth);
}

void xFinal(sqlite3_context *context)
{
    JNIEnv *env = 0;
    jobject *func = 0;
    static jmethodID mth = 0;

    env = ((UDFData*)sqlite3_user_data(context))->env;

    if (!mth) mth = (*env)->GetMethodID(env, aclass, "xFinal", "()V");

    func = sqlite3_aggregate_context(context, sizeof(jobject));
    assert(*func); // disaster

    xCall(context, 0, 0, *func, mth);

    // clean up Function.Aggregate instance
    (*env)->DeleteGlobalRef(env, *func);
}


// INITIALISATION ///////////////////////////////////////////////////

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JNIEnv* env = 0;

    if (JNI_OK != (*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_2))
        return JNI_ERR;

    dbclass = (*env)->FindClass(env, "org/sqlite/DB");
    if (!dbclass) return JNI_ERR;
    dbclass = (*env)->NewGlobalRef(env, dbclass);

    fclass = (*env)->FindClass(env, "org/sqlite/Function");
    if (!fclass) return JNI_ERR;
    fclass = (*env)->NewGlobalRef(env, fclass);

    aclass = (*env)->FindClass(env, "org/sqlite/Function$Aggregate");
    if (!aclass) return JNI_ERR;
    aclass = (*env)->NewGlobalRef(env, aclass);

    return JNI_VERSION_1_2;
}


// WRAPPERS for sqlite_* functions //////////////////////////////////

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
        JNIEnv *env, jobject this)
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
    jint length;
    const void *str = sqlite3_column_text16(toref(stmt), col);
    length = sqlite3_column_bytes16(toref(stmt), col) / 2; // in jchars
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
    assert(jBlob); // out-of-memory

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


JNIEXPORT void JNICALL Java_org_sqlite_DB_result_1null(
        JNIEnv *env, jobject this, jlong context)
{
    sqlite3_result_null(toref(context));
}

JNIEXPORT void JNICALL Java_org_sqlite_DB_result_1text(
        JNIEnv *env, jobject this, jlong context, jstring value)
{
    const jchar *str;
    jsize size;

    if (value == NULL) { sqlite3_result_null(toref(context)); return; }
    size = (*env)->GetStringLength(env, value) * 2;

    str = (*env)->GetStringCritical(env, value, 0);
    assert(str); // out-of-memory
    sqlite3_result_text16(toref(context), str, size, SQLITE_TRANSIENT);
    (*env)->ReleaseStringCritical(env, value, str);
}

JNIEXPORT void JNICALL Java_org_sqlite_DB_result_1blob(
        JNIEnv *env, jobject this, jlong context, jobject value)
{
    jbyte *bytes;
    jsize size;

    if (value == NULL) { sqlite3_result_null(toref(context)); return; }
    size = (*env)->GetArrayLength(env, value);

    // be careful with *Critical
    bytes = (*env)->GetPrimitiveArrayCritical(env, value, 0);
    assert(bytes); // out-of-memory
    sqlite3_result_blob(toref(context), bytes, size, SQLITE_TRANSIENT);
    (*env)->ReleasePrimitiveArrayCritical(env, value, bytes, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_org_sqlite_DB_result_1double(
        JNIEnv *env, jobject this, jlong context, jdouble value)
{
    sqlite3_result_double(toref(context), value);
}

JNIEXPORT void JNICALL Java_org_sqlite_DB_result_1long(
        JNIEnv *env, jobject this, jlong context, jlong value)
{
    sqlite3_result_int64(toref(context), value);
}

JNIEXPORT void JNICALL Java_org_sqlite_DB_result_1int(
        JNIEnv *env, jobject this, jlong context, jint value)
{
    sqlite3_result_int(toref(context), value);
}




JNIEXPORT jstring JNICALL Java_org_sqlite_DB_value_1text(
        JNIEnv *env, jobject this, jobject f, jint arg)
{
    jint length = 0;
    const void *str = 0;
    sqlite3_value *value = tovalue(env, f, arg);
    if (!value) return NULL;

    length = sqlite3_value_bytes16(value) / 2; // in jchars
    str = sqlite3_value_text16(value);
    return str ? (*env)->NewString(env, str, length) : NULL;
}

JNIEXPORT jbyteArray JNICALL Java_org_sqlite_DB_value_1blob(
        JNIEnv *env, jobject this, jobject f, jint arg)
{
    jsize length;
    jbyteArray jBlob;
    jbyte *a;
    const void *blob;
    sqlite3_value *value = tovalue(env, f, arg);
    if (!value) return NULL;

    blob = sqlite3_value_blob(value);
    if (!blob) return NULL;

    length = sqlite3_value_bytes(value);
    jBlob = (*env)->NewByteArray(env, length);
    assert(jBlob); // out-of-memory

    a = (*env)->GetPrimitiveArrayCritical(env, jBlob, 0);
    memcpy(a, blob, length);
    (*env)->ReleasePrimitiveArrayCritical(env, jBlob, a, 0);

    return jBlob;
}

JNIEXPORT jdouble JNICALL Java_org_sqlite_DB_value_1double(
        JNIEnv *env, jobject this, jobject f, jint arg)
{
    sqlite3_value *value = tovalue(env, f, arg);
    return value ? sqlite3_value_double(value) : 0;
}

JNIEXPORT jlong JNICALL Java_org_sqlite_DB_value_1long(
        JNIEnv *env, jobject this, jobject f, jint arg)
{
    sqlite3_value *value = tovalue(env, f, arg);
    return value ? sqlite3_value_int64(value) : 0;
}

JNIEXPORT jint JNICALL Java_org_sqlite_DB_value_1int(
        JNIEnv *env, jobject this, jobject f, jint arg)
{
    sqlite3_value *value = tovalue(env, f, arg);
    return value ? sqlite3_value_int(value) : 0;
}

JNIEXPORT jint JNICALL Java_org_sqlite_DB_value_1type(
        JNIEnv *env, jobject this, jobject func, jint arg)
{
    return sqlite3_value_type(tovalue(env, func, arg));
}


JNIEXPORT jint JNICALL Java_org_sqlite_DB_create_1function(
        JNIEnv *env, jobject this, jstring name, jobject func)
{
    jint ret = 0;;
    const char *strname = 0;
    int isAgg = 0;
    static jfieldID udfdatalist = 0;

    if (!udfdatalist)
        udfdatalist = (*env)->GetFieldID(env, dbclass, "udfdatalist", "J");

    UDFData *udf = malloc(sizeof(struct UDFData));
    assert(udf); // out-of-memory

    isAgg = (*env)->IsInstanceOf(env, func, aclass);

    udf->env = env;
    udf->func = (*env)->NewGlobalRef(env, func);

    // add new function def to linked list
    udf->next = toref((*env)->GetLongField(env, this, udfdatalist));
    (*env)->SetLongField(env, this, udfdatalist, fromref(udf));

    strname = (*env)->GetStringUTFChars(env, name, 0);
    assert(strname); // out-of-memory

    ret = sqlite3_create_function(
            gethandle(env, this),
            strname,       // function name
            -1,            // number of args
            SQLITE_UTF16,  // preferred chars
            udf,
            isAgg ? 0 :&xFunc,
            isAgg ? &xStep : 0,
            isAgg ? &xFinal : 0
    );

    (*env)->ReleaseStringUTFChars(env, name, strname);

    return ret;
}

JNIEXPORT jint JNICALL Java_org_sqlite_DB_destroy_1function(
        JNIEnv *env, jobject this, jstring name)
{
    const char* strname = (*env)->GetStringUTFChars(env, name, 0);
    sqlite3_create_function(
        gethandle(env, this), strname, -1, SQLITE_UTF16, 0, 0, 0, 0
    );
    (*env)->ReleaseStringUTFChars(env, name, strname);
}

JNIEXPORT void JNICALL Java_org_sqlite_DB_free_1functions(
        JNIEnv *env, jobject this)
{
    // clean up all the malloc()ed UDFData instances using the
    // linked list stored in DB.udfdatalist
    jfieldID udfdatalist;
    UDFData *udf, *udfpass;

    udfdatalist = (*env)->GetFieldID(env, dbclass, "udfdatalist", "J");
    udf = toref((*env)->GetLongField(env, this, udfdatalist));
    (*env)->SetLongField(env, this, udfdatalist, 0);

    while (udf) {
        udfpass = udf->next;
        (*env)->DeleteGlobalRef(env, udf->func);
        free(udf);
        udf = udfpass;
    }
}


// COMPOUND FUNCTIONS ///////////////////////////////////////////////

static jint sqlbind(JNIEnv *env, sqlite3_stmt *stmt, jint pos, jobject value)
{
    static jclass Integer = 0, Long, Double, String, ByteArray;
    static jmethodID intValue, longValue, doubleValue;

    jint rc;
    jbyte *a;
    const jchar *str;
    jsize size;

    if (Integer == NULL) {
        // static initialization
        assert(Integer = (*env)->FindClass(env, "java/lang/Integer"));
        assert(Long = (*env)->FindClass(env, "java/lang/Long"));
        assert(Double = (*env)->FindClass(env, "java/lang/Double"));
        assert(String = (*env)->FindClass(env, "java/lang/String"));
        assert(ByteArray = (*env)->FindClass(env, "[B"));

        Integer = (*env)->NewGlobalRef(env, Integer);
        Long = (*env)->NewGlobalRef(env, Long);
        Double = (*env)->NewGlobalRef(env, Double);
        String = (*env)->NewGlobalRef(env, String);
        ByteArray = (*env)->NewGlobalRef(env, ByteArray);

        intValue = (*env)->GetMethodID(env, Integer, "intValue", "()I");
        longValue = (*env)->GetMethodID(env, Long, "longValue", "()J");
        doubleValue = (*env)->GetMethodID(env, Double, "doubleValue", "()D");

        assert(intValue);
        assert(longValue);
        assert(doubleValue);
    }

    if (value == NULL) {
        rc = sqlite3_bind_null(stmt, pos);
    } else if ((*env)->IsInstanceOf(env, value, Integer)) {
        rc = sqlite3_bind_int(stmt, pos, 
            (*env)->CallIntMethod(env, value, intValue));
    } else if ((*env)->IsInstanceOf(env, value, Long)) {
        rc = sqlite3_bind_int64(stmt, pos, 
            (*env)->CallLongMethod(env, value, longValue));
    } else if ((*env)->IsInstanceOf(env, value, Double)) {
        rc = sqlite3_bind_double(stmt, pos, 
            (*env)->CallDoubleMethod(env, value, doubleValue));
    } else if ((*env)->IsInstanceOf(env, value, String)) {
        size = (*env)->GetStringLength(env, value) * 2; // in bytes
        assert(str = (*env)->GetStringCritical(env, value, 0));
        rc = sqlite3_bind_text16(stmt, pos, str, size, SQLITE_TRANSIENT);
        (*env)->ReleaseStringCritical(env, value, str);

    } else if ((*env)->IsInstanceOf(env, value, ByteArray)) {
        size = (*env)->GetArrayLength(env, value);
        assert(a = (*env)->GetPrimitiveArrayCritical(env, value, 0));
        rc = sqlite3_bind_blob(stmt, pos, a, size, SQLITE_TRANSIENT);
        (*env)->ReleasePrimitiveArrayCritical(env, value, a, JNI_ABORT);

    } else {
        assert(0);
    }

    return rc;
}

JNIEXPORT jintArray JNICALL Java_org_sqlite_DB_executeBatch(
        JNIEnv *env, jobject this, jlong stmt, jobjectArray values)
{
    jintArray changes;
    jobject val;
    jint i, j, *c, rc, params_count, batch_count, batch_pos, values_length;
    sqlite3 *db = gethandle(env, this);
    sqlite3_stmt *dbstmt = toref(stmt);

    params_count = sqlite3_bind_parameter_count(dbstmt);
    values_length = (*env)->GetArrayLength(env, values);
    batch_count = values_length / params_count;

    c = calloc(batch_count, sizeof(jint));
    assert(c); // out-of-memory

    for (i=0; i < batch_count; i++) {
        batch_pos = i * params_count;
        sqlite3_reset(dbstmt);

        for (j=0; j < params_count; j++) {
            val = (*env)->GetObjectArrayElement(env, values, batch_pos + j);
            sqlbind(env, dbstmt, j + 1, val);
        }

        rc = sqlite3_step(dbstmt);
        if (rc != SQLITE_DONE)
            break;
        c[i] = sqlite3_changes(db);
    }

    // handle error
    if (rc != SQLITE_DONE) {
        if (rc == SQLITE_ERROR)
            rc = sqlite3_reset(dbstmt);
        // TODO throw ex, handle schema in loop
    }


    changes = (*env)->NewIntArray(env, batch_count);
    assert(changes); // out-of-memory
    (*env)->SetIntArrayRegion(env, changes, 0, batch_count, c);
    return changes;
}

JNIEXPORT jboolean JNICALL Java_org_sqlite_DB_execute(
        JNIEnv *env, jobject this, jlong stmt, jobjectArray values)
{
    jobject val;
    jint rc, i, values_length;
    sqlite3_stmt *dbstmt = toref(stmt);

    values_length = values ? (*env)->GetArrayLength(env, values) : 0;
    assert(values_length == sqlite3_bind_parameter_count(dbstmt));

    for (i=0; i < values_length; i++) {
        val = (*env)->GetObjectArrayElement(env, values, i);
        sqlbind(env, dbstmt, i + 1, val);
    }

    rc = sqlite3_step(dbstmt);
    if (rc == SQLITE_ERROR)
        rc = sqlite3_reset(dbstmt);

    switch (rc) {
        case SQLITE_DONE:
            return JNI_FALSE;
        case SQLITE_ROW:
            return JNI_TRUE;
        case SQLITE_BUSY:
            throwexmsg(env, "database locked"); break;
        case SQLITE_MISUSE:
            throwexmsg(env, "jdbc internal consistency error"); break;
        case SQLITE_SCHEMA: // TODO
            /*sqlite3_transfer_bindings(dbstmt, newdbstmt);
            return Java_org_sqlite_DB_execute(
                    env, this, fromref(newdbstmt), values);*/
        case SQLITE_INTERNAL: // TODO: be specific
        case SQLITE_PERM:
        case SQLITE_ABORT:
        case SQLITE_NOMEM:
        case SQLITE_READONLY:
        case SQLITE_INTERRUPT:
        case SQLITE_IOERR:
        case SQLITE_CORRUPT:
        case SQLITE_ERROR:
        default:
            throwex(env, this);
    }

    return JNI_FALSE;
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
    assert(array); // out-of-memory

    colDataRaw = (jboolean*)malloc(3 * sizeof(jboolean));
    assert(colDataRaw); // out-of-memory


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
        assert(colData); // out-of-memory

        (*env)->SetBooleanArrayRegion(env, colData, 0, 3, colDataRaw);
        (*env)->SetObjectArrayElement(env, array, i, colData);
    }

    free(colDataRaw);

    return array;
}

