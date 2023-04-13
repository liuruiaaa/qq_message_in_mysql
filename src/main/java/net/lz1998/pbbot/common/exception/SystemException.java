package net.lz1998.pbbot.common.exception;

import net.lz1998.pbbot.common.enums.CCBErrorCode;

/**
 * 成都思致科技有限公司
 *
 * @author Changge Zhang
 * @date 2022/5/24 10:24
 * =========================================================================
 * 变更履历：
 * -------------------------------------------------------------------------
 * 变更编号     变更时间    变更人   变更原因    变更内容
 * <p>
 * -------------------------------------------------------------------------
 */
public class SystemException extends RuntimeException {

    /**
     * 返回码
     */
    private String retCode = CCBErrorCode.FAILED_TRADE.getCode();

    public String getRetCode() {
        return retCode;
    }

    /**
     * Constructs a new runtime exception with {@code null} as its
     * detail message.  The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}.
     */
    public SystemException() {
    }

    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public SystemException(String message) {
        super(message);
    }
    public SystemException(String retCode, String message) {
        super(message);
        this.retCode = retCode;
    }
    public SystemException(String retCode, String message, String... args) {
        super(String.format(message,args));
        this.retCode = retCode;
    }

    /**
     * Constructs a new runtime exception with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).  (A <tt>null</tt> value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     * @since 1.4
     */
    public SystemException(String message, Throwable cause) {
        super(message, cause);
    }
    public SystemException(String retCode, String message, Throwable cause) {
        super(message, cause);
        this.retCode = retCode;
    }
    public SystemException(String retCode, String message, Throwable cause, String... args) {
        super(String.format(message,args),cause);
        this.retCode = retCode;
    }

    /**
     * Constructs a new runtime exception with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).  (A <tt>null</tt> value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     * @since 1.4
     */
    public SystemException(Throwable cause, String message) {
        super(message, cause);
    }
    public SystemException(Throwable cause, String retCode, String message) {
        super(message, cause);
        this.retCode = retCode;
    }

    /**
     * Constructs a new runtime exception with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).  (A <tt>null</tt> value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     * @since 1.4
     */
    public SystemException(Throwable cause, String message, String... args) {
        super(String.format(message,args), cause);
    }
    public SystemException(Throwable cause, String retCode, String message, String... args) {
        super(String.format(message,args), cause);
        this.retCode = retCode;
    }

    /**
     * Constructs a new runtime exception with the specified detail
     * message, cause, suppression enabled or disabled, and writable
     * stack trace enabled or disabled.
     *
     * @param message            the detail message.
     * @param cause              the cause.  (A {@code null} value is permitted,
     *                           and indicates that the cause is nonexistent or unknown.)
     * @param enableSuppression  whether or not suppression is enabled
     *                           or disabled
     * @param writableStackTrace whether or not the stack trace should
     *                           be writable
     * @since 1.7
     */
    public SystemException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    public SystemException(String retCode, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.retCode = retCode;
    }
}
