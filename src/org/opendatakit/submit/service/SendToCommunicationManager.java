package org.opendatakit.submit.service;

import java.util.ArrayList;
import java.util.LinkedList;

import org.opendatakit.submit.data.SubmitObject;
import org.opendatakit.submit.flags.CommunicationState;
import org.opendatakit.submit.flags.Radio;
import org.opendatakit.submit.route.CommunicationManager;

import android.util.Log;

/**
 * sendToManager Runnable
 * Gets passed to the routeInBackgroundThread;
 * based on the TYPE of the object on the top of the queue
 * it passes off to the MessageManager or the SyncManager
 */
public class SendToCommunicationManager implements Runnable {

	private final String TAG = SendToCommunicationManager.class.getName();
	private SubmitService mService = null;
	private SubmitQueue mSubmitQueue = null;
	private CommunicationManager mManager = null;
	private Radio mActiveRadio = null;
	
	// TODO change ArrayList to SubmitQueue class type
	public SendToCommunicationManager(SubmitService service) {
		mService = service;
	}
	
	@Override
	public void run() {
		Log.i(TAG, "Starting to run sendToManagerThread");
		
		
		// While there are submission requests in the Queue, service the queue
		// with appropriate calls to executeTask() from the MessageManager or SyncManager
		while(!Thread.currentThread().isInterrupted()) { // TODO this is a bit brute force-ish, but it will do for the moment
			mSubmitQueue = mService.getSubmitQueue();
			mActiveRadio = mService.getActiveRadio();
			mManager = mService.getCommunicationManager();
			try {
				if (mService.getSubmitQueueSize() < 1) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						Log.e(TAG, e.getMessage());
					}
					continue;
				}
				CommunicationState result = null;
				SubmitObject top = mService.popFromSubmitQueue();
				// Check if there is an ordered element before
				// passing it off to the CommunicationManager
				
				result = (CommunicationState)mManager.route(top, mActiveRadio);
				
				
				// Depending on the resulting CommunicationState
				// pop the top object off mSubmitQueue, keep it in for 
				// another round, or pop it and throw an exception
				switch(result) {
					case CHANNEL_UNAVAILABLE:
						// For now, we are not removing anything from the
						// record keeping data structures.
						Log.i(TAG, "Result was " + result.toString());
						mService.addLastToSubmitQueue(top);
						break;
					case SEND:
						// For now, we are not removing anything from the
						// record keeping data structures.
						Log.i(TAG, "Result was " + result.toString());
						mService.broadcastStateToApp(top, result);
						mService.addLastToSubmitQueue(top);
						break;
					case WAITING_ON_APP_RESPONSE:
						// For now, we are not removing anything from the
						// record keeping data structures.
						Log.i(TAG, "Result was " + result.toString());
						mService.broadcastStateToApp(top, result);
						mService.addLastToSubmitQueue(top);
						break;
					case SUCCESS:
						// For now, we are not removing anything from the
						// record keeping data structures.
						Log.i(TAG, "Result was " + result.toString());
						top.setState(result);
						if(top.getAddress() != null) {
							mService.broadcastStateToApp(top, result);
						}
						break;
					case FAILURE_RETRY:
						// For now, we are not removing anything from the
						// record keeping data structures.
						Log.i(TAG, "Result was " + result.toString());
						top.setState(result);
						if(top.getAddress() != null) {
							mService.broadcastStateToApp(top, result);
						}
						mService.addLastToSubmitQueue(top);
						break;
					case FAILURE_NO_RETRY:
						// For now, we are not removing anything from the
						// record keeping data structures.
						Log.i(TAG, "Result was " + result.toString());
						top.setState(result);
						if(top.getAddress() != null) {
							mService.broadcastStateToApp(top, result);
						}
						break;
					default:
						top.setState(CommunicationState.FAILURE_NO_RETRY);
						mService.broadcastStateToApp(top, result);
						break;
				}
			} catch (Exception e) {
				String err = (e.getMessage() == null)?"Exception":e.getMessage();
				e.printStackTrace();
				return;
			} 
			// Add downtime
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Log.e(TAG, e.getMessage());
				break;
			} // TODO temporary time
			continue;
		}
		Log.i(TAG, "Thread has finished run()");
	}
	
}