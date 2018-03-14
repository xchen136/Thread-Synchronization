# Thread-Synchronization (Cs340 - Operating System, Professor Vivek Upadhyay, Spring 2017)
A random matching game that uses semaphore to synchronize processes.

"Project Idea"
Match can only happen when there are 3 women with 1 man, or 3 men with 1 woman arriving in the room. 
The match will happen between the single gender person with the opposite gender person who arrives second. 
The single gender person's arrival order is not taken into account. For instance, if there are 3men 1woman, 
then the 1woman will match with Men#2 who comes in 2nd within the men. 
They will not know their arrival order until they have arrived. 
Once a match happens, everyone need to leave the room and the unmatched need to enter another match again. 
However, the unmatched need to wait in line if there are others already waiting for match.

