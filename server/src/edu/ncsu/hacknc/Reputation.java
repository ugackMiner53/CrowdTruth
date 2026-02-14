package edu.ncsu.hacknc;

import java.util.ArrayList;

/**
 * Reputation for Post
 * 
 * @author Arianna Nugent-Freeman
 * @author
 */
public class Reputation {
	
	/** Counter for Post Reputation*/
	private int count = 0;
	
	/**Account who interact with Post*/
	private ArrayList<String> interactedAccount = new ArrayList<String>(); 
	
	/**
	 * Post position when liked
	 * 
	 * @param Account User with Account
	 */
	public void likePost(String Account) {
		//POST IS EMPTY, COME BACK TOO ONCE FILLED
		if (interactedAccount.contains(Account)) {
			throw new IllegalArgumentException(); // really just return a network call of BAD REQUEST
		} else {
			count++;
		}
		
	}
	
	/**
	 * Post position when disliked
	 * 
	 * @param Account User with Account
	 */
	public void dilikePost(String Account) {
		//POST IS EMPTY, COME BACK TOO ONCE FILLED
		if (interactedAccount.contains(Account)) {
			throw new IllegalArgumentException(); // really just return a network call of BAD REQUEST
		} else {
			count--;
		}
		
	}
	
	/**
	 * Keeps track of Post being True or False
	 */
	public void tracker() {
		//Come back to and set correct message/field
		if (count <= 0) {
			System.out.println("Post Contains False Information");
		}
		if (count > 0 && count <= 3) {
			System.out.println("Post Information Needs Review");
		}
		if (count > 3) {
			System.out.println("Post Contains True Information");
		}
	}
	

}
