package edu.ncsu.hacknc;

import java.util.ArrayList;

/**
 * Reputation for Post
 * 
 * @author Arianna Nugent-Freeman
 */
public class Reputation {
	
	/** Counter for Post Reputation*/
	private int count = 0;
	
	/**Account who interact with Post*/
	private ArrayList<String> interactedAccount = new ArrayList<String>(); 
	
	/**
	 * Post position when liked
	 * 
	 * @param user Account user
	 */
	public void likePost(Account user) {
		if (interactedAccount.contains(user.getId())) {
			throw new IllegalArgumentException(); // really just return a network call of BAD REQUEST
		} else {
			count++;
		}
		
	}
	
	/**
	 * Post position when disliked
	 * 
	 * @param user Account user
	 */
	public void dilikePost(Account user) {
		if (interactedAccount.contains(user.getId())) {
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
