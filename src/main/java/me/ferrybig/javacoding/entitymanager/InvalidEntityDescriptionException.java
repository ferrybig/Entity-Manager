/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.entitymanager;

/**
 *
 * @author admin
 */
public class InvalidEntityDescriptionException extends RuntimeException {

	private static final long serialVersionUID = 8654353255119354863L;

	/**
	 * Creates a new instance of <code>InvalidEntityDescriptionException</code>
	 * without detail message.
	 */
	public InvalidEntityDescriptionException() {
	}

	/**
	 * Constructs an instance of <code>InvalidEntityDescriptionException</code>
	 * with the specified detail message.
	 *
	 * @param msg the detail message.
	 */
	public InvalidEntityDescriptionException(String msg) {
		super(msg);
	}
	
	 /**
	 * Constructs an instance of <code>InvalidEntityDescriptionException</code> with the specified
	 * detail message and cause.
	 *
	 * @param message the detail message.
	 * @param cause the cause
	 */
	public InvalidEntityDescriptionException(String message, Throwable cause) {
		super(message);
        this.initCause(cause);
	}

	/**
	 * Constructs an instance of <code>InvalidEntityDescriptionException</code> with the specified
	 * cause.
	 *
	 * @param cause the cause
	 */
	public InvalidEntityDescriptionException(Throwable cause) {
		this(cause.toString());
        this.initCause(cause);
	}
}
