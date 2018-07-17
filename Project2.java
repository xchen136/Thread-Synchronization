/*Cs340 - Project2 by Xiaomin Chen (14145453) & Xin Jin
 *Below is the synchronization problem:
 *Match can only happen when there are 3 women with 1 man, or 3 men with 1 woman arriving in the room. 
 *The match will happen between the single gender person with the opposite gender person who arrives second. 
 *The single gender person's arrival order is not taken into account. For instance, if there are 3men 1woman, 
 *then the 1woman will match with Men#2 who comes in 2nd within the men. 
 *They will not know their arrival order until they have arrived. 
 *Once a match happens, everyone need to leave the room and the unmatched need to enter another match again. 
 *However, the unmatched need to wait in line if there are others already waiting for match.
 */

import java.util.Scanner;

class Semaphore
{
    private int value;
    
    public Semaphore(int value) {
	this.value = value;
    }
    
    public synchronized void acquire() {
	while (value <= 0) {
	    try {
		wait();
	    }
	    catch (InterruptedException e) { }
	}
	
	value--;
    }

    public synchronized void release() {
	++value;
	
	notify();
    }
    
    public int getValue(){
	return value;
    }
}

class SleepUtilities
{
    /**
     * Nap between zero and NAP_TIME seconds.
     */
    public static void nap() {
	nap(NAP_TIME);
    }
    
    /**
     * Nap between zero and duration seconds.
     */
    public static void nap(int duration) {
	int sleeptime = (int) (NAP_TIME * Math.random() );
	try { Thread.sleep(sleeptime*1000); }
	catch (InterruptedException e) {}
    }
    
    private static final int NAP_TIME = 5;
}

interface EnterLeave
{
    public boolean enterMatch(String gender, int identification);
    public boolean leaveMatch(String gender, int identification);
}

class female implements Runnable
{
    private int id;
    private EnterLeave match;
    private boolean dating;
    
    female(int num, EnterLeave m){
	this.id = num;
	this.match = m;
	this.dating = false;							     	//initially unmatched
    }
    
    public void run(){
	SleepUtilities.nap();								//delay for fair starting point
	
	while(!dating){
	    
	    if(match.enterMatch("Female", id)){						//if enterRoom, leave room
		dating = match.leaveMatch("Female", id);				//leave match with dating status
	    }
	    else{
		SleepUtilities.nap();						        //delay to let eligible participants enter first
	    }
	}
    }//once matched, thread finishes
    
}

class male implements Runnable
{
    private int id;
    private EnterLeave match;
    private boolean dating;
    
    male(int num, EnterLeave m){
	this.id = num;
	this.match = m;
	this.dating = false;
    }
    
    public void run(){
	
	SleepUtilities.nap();						      		//delay for fair starting point
	
	while(!dating){
	    
	    if(match.enterMatch("Male", id)){					     	//if enterRoom, leave room
		dating = match.leaveMatch("Male", id);					//leave match with dating status
	    }	
	    else{
		SleepUtilities.nap();							//delay to let eligible participants enter first
	    }
	}//once matched, thread finishes
    }
}

class room implements EnterLeave
{	
    private String threeGender;					      			//gender of the three
    private String oneGender;		       						//gender of the only participant
    private QueueLL roomMatch;	       						      	//queue used  to keep track of participants who entered match
    private int countMen;					      			//count of male participants
    private int countWomen;								//count of female participants
    private int total;									//total number of participants
    private int match;									//keep track of the number of match
    private int secondThree;								//keep track of the participant who entered second within the three
    private static final int participants = 4;					      	//match only allows 4 eligible participants
    private boolean targetSet;				      				//whether the oneGender and threeGender is determined
    private boolean[] women;								//keep track of matching status
    private boolean[] men;	
    private Semaphore enter;					      			//checks if match is open
    private Semaphore mutexE;						                //mutual exclusion within between singles who try to enter match
    private Semaphore mutexL;  		       			//separate semaphore so singles can still leave room while singles try to enter match
    private Semaphore leave;	    				         		//only can leave when match have gathered 4 eligible participants
    
    room(int numF, int numM){		
	roomMatch = new QueueLL();
	countMen = 0;
	countWomen = 0;
	total = 0;
	women = new boolean[numF];						        //initially all false, all singles are unmatched
	men = new boolean[numM];
	enter = new Semaphore(participants);						//match needs 4 participants
	mutexE = new Semaphore(1);
	mutexL = new Semaphore(1);
	leave = new Semaphore(0);						        //cannot leave until the last eligible participant have arrived
	
	if(limitedSingles()){	     		                //if a gender have <3  participants available, then it must be set as the oneGender
	    setGenders();
	}
	else{	     									//else, no gender limitation are set yet
	    oneGender = "";
	    threeGender = "";
	    targetSet = false;
	}
    }
    
    public boolean enterMatch(String gender, int identification){	
	int oneID;
	node newNode;
	mutexE.acquire();							        //mutual exclusion for concurrent singles try to enter match
	enter.acquire();						      		//check if match is available
	
	System.out.println(gender + identification + " tries to enter room for match.");
	if(!targetSet || eligible(gender)){		      							
	    ++total;
	    newNode = new node(gender, identification);
	    roomMatch.enqueue(newNode);
	    roomMatch.printList();
	    if(gender == "Female"){		
		++countWomen;
		if(countWomen == 2){						        //if it came in second, genderLimitation can be determined
		    threeGender = "Female"; 
		    oneGender = "Male";
		    secondThree = identification;
		    targetSet = true;
		}
	    }
	    else{		
		++countMen;
		if(countMen == 2){
		    threeGender = "Male";
		    oneGender = "Female";
		    secondThree = identification;
		    targetSet = true;
		}
	    }
	}
	else{														      				
	    enter.release();											
	    System.out.println(gender + identification + " is currently ineligible to enter room for match.");			
	    mutexE.release();		
	    return false;		                                           	//fails to enter match	
	}	
	
	//after the last participant arrives, matching result is printed
	if(total == 4){
	    oneID = roomMatch.findTarget(oneGender);
	    roomMatch.delete(oneGender, oneID);
	    if(oneGender == "Female"){
		men[secondThree-1] = true;
		women[oneID-1] = true;
		roomMatch.delete("Male", secondThree);
	    }
	    else{
		women[secondThree-1] = true;
		men[oneID-1] = true;
		roomMatch.delete("Female", secondThree);
	    }
	    
	    System.out.println("\n[Match " + (++match) + "]");
	    System.out.println(threeGender + secondThree + " is matched with " + oneGender + oneID);
	    
	    node unmatched;
	    while(!roomMatch.isEmpty()){
		unmatched = roomMatch.dequeue();
		System.out.println(unmatched.getGender() + unmatched.getID() + " is unmatched.");
	    }
	    System.out.println("");
	    
	    if(!matchPossible()){
		System.out.println("**Matches are over**");
		printUnmatched();
		System.exit(0);
	    }
	    
	    mutexE.release();
	    
	    for(int i=1; i<participants; i++){			                       //allow the first 3 participants to leave the match
		leave.release();
	    }
	}
	else{
	    mutexE.release();
	    leave.acquire();	                   	             //can only leave after last participant have arrived to keep count of total consistent
	}
	
	return true;
    }
    
    //once match is over, everyone in the room leaves
    public boolean leaveMatch(String gender, int identification){
	
	mutexL.acquire();
	--total;
	
	//last one leaving will reset values and leave the door open for new match
	if(total == 0){		
	    
	    System.out.println("All participants left the room.");
	    countMen = 0;
	    countWomen = 0;
	    if(limitedSingles()){
		setGenders();
	    }
	    else{
		oneGender = "";
		threeGender = "";
		targetSet = false;
	    }
	    
	    for(int i=1; i<=participants; i++){
		enter.release();
	    }
	}
	
	mutexL.release();
	
	//return matching status to determine whether to enter room for match again
	if(gender == "Female"){
	    return women[identification-1];
	}
	else{
	    return men[identification-1];
	}
    }
    
    //match is only possible if there is exists 1 participant in one gender, and 3 participants in another gender
    public boolean matchPossible(){
	int fSingles = unmatchedCount(women);
	int mSingles = unmatchedCount(men);
	
	if((fSingles >= 1 && mSingles >=3) || (mSingles >= 1 && fSingles >= 3))
	    return true;
	else
	    return false;
	
    }
    
    public void printUnmatched(){
	System.out.print("Females who cannot be matched:  ");
	for(int f=0; f<women.length; f++){
	    if(women[f] == false){
		System.out.print((f+1) + ", ");
	    }
	}
	System.out.print("\nMales who cannot be matched:  ");
	for(int m=0; m<men.length; m++){
	    if(men[m] == false){
		System.out.print((m+1) + ", ");
	    }
	}
	System.out.println("\n");
    }
    
    public boolean eligible(String gen){
	if(oneGender == gen && oneGender == "Female" && countWomen<1){
	    return true;
	}
	else if(oneGender == gen && oneGender == "Male" && countMen<1){
	    return true;
	}
	else if(threeGender == gen && threeGender == "Female" && countWomen<3){
	    return true;
	}
	else if(threeGender == gen && threeGender == "Male" && countMen<3){
	    return true;
	}
	else
	    return false;
    }
    
    public boolean limitedSingles(){
	if(unmatchedCount(women)<3 || unmatchedCount(men)<3){
	    return true;
	}
	return false;
    }
    
    public void setGenders(){
	if(unmatchedCount(women) < 3){
	    oneGender = "Female";
	    threeGender = "Male";
	    targetSet = true;
	}
	else{
	    oneGender = "Male";
	    threeGender = "Female";
	    targetSet = true;
	}
    }
    
    public int unmatchedCount(boolean[] x){
	int count = 0;
	for(int i=0; i<x.length; i++){
	    if(x[i] == false){
		++count;
	    }
	}
	return count;
    }
    
}

class node
{
    private String gender;
    private int id;
    private node next = null;
    
    node(){}
    
    node(String g, int i){
	this.gender = g;
	this.id = i;
    }
    
    public String getGender(){
	return gender;
    }
    
    public int getID(){
	return id;
    }
    
    public node getNext(){
	return next;
    }
    
    public void setNext(node n){
	next = n;
    }
    
}

class QueueLL
{
    private node head;
    private node tail;
    private int length;
    
    QueueLL(){
	node dummy = new node();
	this.head = dummy;
	this.tail = dummy;
	this.length = 0;
    }
    
    //insert at the end
    public void enqueue(node newItem){			
	if(isEmpty()){
	    head = newItem;									
	    tail = newItem;
	}
	else{
	    node temp = tail;
	    temp.setNext(newItem);
	    tail = newItem;
	}
	++length;
    }
    
    //remove from head
    public node dequeue(){
	if(!isEmpty()){
	    node temp = head;
	    head = head.getNext();
	    temp.setNext(null);
	    --length;
	    return temp;
	}
	return null;
    }
    
    //delete a specific node using gender and id
    public void delete(String g, int i){
	node temp;
	if(head.getGender() == g && head.getID() == i){	      												
	    temp = head;
	    head = head.getNext();
	    temp.setNext(null);
	    --length;
	    return;
	}
	node current = head;
	while(current.getNext() != null){      														
	    if(current.getNext().getGender() != g || current.getNext().getID() != i){
		current = current.getNext();													
		continue;
	    }	
	    else{	    																
		temp = current.getNext();
		current.setNext(temp.getNext());
		temp.setNext(null);
		--length;
		return;
	    }
	}
    }
    
    //simply to find the id of the only one and only targetGender
    public int findTarget(String g){
	if(isEmpty()){
	    return -1;
	}
	node current = head;
	while(current != null){
	    if(current.getGender() == g){
		return current.getID();
	    }
	    else{
		current = current.getNext();
	    }
	}
	return -2;
    }
    
    public void printList(){
	node current;
	if(!isEmpty()){
	    current = head;
	    System.out.print("(Room:  ");
	    while(current != null){
		System.out.print(current.getGender() + current.getID() + " ");
		current = current.getNext();
	    }
	    System.out.println(")");
	}
    }
    
    public boolean isEmpty(){
	if(length == 0){
	    return true;
	}
	return false;
    }
}

public class Project2 {
    
    public static void main(String[] args){
	Scanner input = new Scanner(System.in);
	EnterLeave matchingRoom;
	int numFemale = 0;
	int numMale = 0;
	
	System.out.print("\nWelcome to Random Matching!\nMatch can only happen when there are four eligible participants in the room,");
        System.out.print("\nwhere one gender is alone with three other genders.\nPlease decide the number of females and males.\n");
	System.out.print("Females: ");

	if(input.hasNextInt()){
	    numFemale = input.nextInt();
	}
	else{
	    System.err.println("Match is not possible. Try again.\n");
	    System.exit(0);
	}
	
	System.out.print("Males: ");
	if(input.hasNextInt()){
	    numMale = input.nextInt();
	}
	else{
	    System.err.println("Match is not possible. Try again.\n");
	    System.exit(0);
	}
	
	//first checks if match is possible with initial counts
	if((numFemale < 1 || numMale < 3) && (numFemale < 3 || numMale < 1)){
	    System.err.println("Match is not possible. Try again.\n");
	    System.exit(0);
	}

	System.out.println("");
	matchingRoom = new room(numFemale, numMale);
	
	for(int i=1; i<=numFemale; i++){
	    Thread f = new Thread(new female(i, matchingRoom));
	    f.start();
	}
	
	for(int i=1; i<=numMale; i++){
	    Thread m = new Thread(new male(i, matchingRoom));
	    m.start();
	}
	
	input.close();
    }
	
}
