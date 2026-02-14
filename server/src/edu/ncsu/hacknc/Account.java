package edu.ncsu.hacknc;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Sets up Accounts for Users
 * 
 * @author Arianna Nugent-Freeman
 */
public class Account {

	/** User id */
	private String id;

	/** User email */
	private String email;

	/** User password */
	private String password;
	
	/**
	 * Construct Account for User with id, email, and password
	 * 
	 * @param id User Id
	 * @param email User Email
	 * @param password User Password
	 */
	public Account(String id, String email, String password) {
		setId(id);
		setEmail(email);
		setPassword(password);
	}


	/**
	 * Get Id
	 * 
	 * @return the id
	 */
	public String getId() {
		return id;
	}


	/**
	 * Set id
	 * 
	 * @param id the id to set
	 */
	public void setId(String id) {
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("Invalid Id");
		}
		
		this.id = id;
	}


	/**
	 * Get email
	 * 
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}


	/**
	 * Set email
	 * 
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		if (email == null || email.isEmpty()) {
			throw new IllegalArgumentException("Invalid Email");
		}
		
		//Must have '@' and '.'
		int at = email.indexOf('@');
		int dot = email.lastIndexOf('.');

		if (at == -1 || dot == -1) {
			throw new IllegalArgumentException("Invalid Email");
		}

		if (dot < at) {
			throw new IllegalArgumentException("Invalid Email");
		}
		
		this.email = email;
	}


	/**
	 * Get password
	 * 
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}


	/**
	 * Set password
	 * 
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		if (password == null || password.isEmpty()) {
			throw new IllegalArgumentException("Invalid Password");
		}
		
		//Must have Capital Letter
		Pattern cap1 = Pattern.compile("[A-Z]");
		Matcher match1 = cap1.matcher(password);
		boolean capLetter = match1.find();
		if (! capLetter) {
			throw new IllegalArgumentException("Invalid Password, Must Contain at least one Capitial Letter");
		}
		
		//Must have Lower Case Letter
		Pattern cap2 = Pattern.compile("[a-z]");
		Matcher match2 = cap2.matcher(password);
		boolean lowerLetter = match2.find();
		if (! lowerLetter) {
			throw new IllegalArgumentException("Invalid Password, Must Contain at least one Lowercase Letter");
		}
		
		//Must has Special Character
		for (int i = 0; i < password.length(); i++) {
			char c = password.charAt(i);
			if ("!?@#$%&".indexOf(c) == -1) {
				throw new IllegalArgumentException("Invalid Password, Must Contain at least one Special Character(!?@#$%&)");
			}
		}
		
		this.password = password;
	}


	/**
	 * Account hashCode
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((password == null) ? 0 : password.hashCode());
		return result;
	}


	/**
	 * Account Equals
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Account other = (Account) obj;
		if (email == null) {
			if (other.email != null)
				return false;
		} else if (!email.equals(other.email))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		return true;
	}

}
