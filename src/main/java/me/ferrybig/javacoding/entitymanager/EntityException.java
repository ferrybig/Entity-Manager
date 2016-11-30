
package me.ferrybig.javacoding.entitymanager;

/**
 *
 * @author Fernando
 */
public class EntityException extends Exception {

    /**
     * Creates a new instance of <code>EntityException</code> without detail message.
     */
    public EntityException() {
        super();
    }


    /**
     * Constructs an instance of <code>EntityException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public EntityException(String msg) {
        super(msg);
    }

    /**
	 * Constructs an instance of <code>EntityException</code> with the specified
	 * detail message and cause.
	 *
	 * @param message the detail message.
	 * @param cause the cause
	 */
	public EntityException(String message, Throwable cause) {
		super(message);
        this.initCause(cause);
	}

	/**
	 * Constructs an instance of <code>EntityException</code> with the specified
	 * cause.
	 *
	 * @param cause the cause
	 */
	public EntityException(Throwable cause) {
		this(cause.toString());
        this.initCause(cause);
	}
}
