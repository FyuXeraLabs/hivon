/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.workers;

import ui.dialogs.ProgressDialog;

import javax.swing.*;

import core.logging.Logger;

/**
 * abstract background task with progress dialog
 * extend this class and implement doInBackground() for db operations
 * 
 * @author Sanod
 */
public abstract class BackgroundTask extends SwingWorker<Boolean, String> {
    
    private final ProgressDialog progressDialog;
    private final JFrame parentFrame;
    private final String taskName;
    
    public BackgroundTask(JFrame parentFrame, String taskName) {
        this.parentFrame = parentFrame;
        this.taskName = taskName;
        this.progressDialog = new ProgressDialog(parentFrame, true);
    }
    
    /**
     * override this method with database/long-running operation
     * return true for success, false for failure
     */
    protected abstract Boolean performTask() throws Exception;
    
    /**
     * override this to handle success
     * default shows success message
     */
    protected void onSuccess() {
        JOptionPane.showMessageDialog(parentFrame, taskName + " completed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * override this to handle failure
     * default shows error message
     */
    protected void onFailure(Exception e) {
        Logger.errlog(taskName + " failed: " + e.getMessage(), e);
        JOptionPane.showMessageDialog(parentFrame, taskName + " failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * override this to perform UI updates after completion
     * called regardless of success/failure
     */
    protected void onComplete() {
        // override in subclass if needed
    }
    
    /**
     * update progress message (call from performTask)
     */
    protected void updateProgress(String message) {
        publish(message);
    }
    
    /**
     * execute the task and show progress dialog
     */
    public void executeWithDialog() {
        // config progress dialog
        progressDialog.setMessage(taskName + "...");
        progressDialog.setStatus("Please wait...");
        
        // execute the worker
        super.execute();
        
        // show modal dialog
        progressDialog.setVisible(true);
    }
    
    @Override
    protected Boolean doInBackground() throws Exception {
        try {
            Logger.log("BackgroundTask", taskName + " started");
            return performTask();
        } catch (Exception e) {
            Logger.errlog(taskName + " error: " + e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    protected void process(java.util.List<String> chunks) {
        // update status with latest message
        if (!chunks.isEmpty()) {
            String latestMessage = chunks.get(chunks.size() - 1);
            progressDialog.setStatus(latestMessage);
        }
    }
    
    @Override
    protected void done() {
        // close progress dialog
        progressDialog.closeDialog();
        
        try {
            // get result
            Boolean success = get();
            
            if (success != null && success) {
                onSuccess();
            } else {
                onFailure(new Exception("Operation returned false"));
            }
            
        } catch (Exception e) {
            onFailure(e);
        } finally {
            onComplete();
        }
        
        Logger.log("BackgroundTask", taskName + " finished");
    }
}