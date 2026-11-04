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
    
    private ProgressDialog progressDialog;
    private final java.awt.Component parentComponent;
    private final String taskName;
    private boolean retryOnFailure = false;
    
    public BackgroundTask(JFrame parentFrame, String taskName) {
        this.parentComponent = parentFrame;
        this.taskName = taskName;
        this.progressDialog = new ProgressDialog(parentFrame, true);
    }
    
    public BackgroundTask(JDialog parentDialog, String taskName) {
        this.parentComponent = parentDialog;
        this.taskName = taskName;
        this.progressDialog = new ProgressDialog(parentDialog, true);
    }

    public BackgroundTask(java.awt.Component parentComponent, String taskName) {
        this.parentComponent = parentComponent;
        this.taskName = taskName;
        java.awt.Window window = parentComponent instanceof java.awt.Window
                ? (java.awt.Window) parentComponent
                : (parentComponent != null ? javax.swing.SwingUtilities.getWindowAncestor(parentComponent) : null);
        if (window instanceof javax.swing.JDialog) {
            this.progressDialog = new ProgressDialog((javax.swing.JDialog) window, true);
        } else if (window instanceof java.awt.Frame) {
            this.progressDialog = new ProgressDialog((java.awt.Frame) window, true);
        } else {
            this.progressDialog = new ProgressDialog((java.awt.Frame) null, true);
        }
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
        JOptionPane.showMessageDialog(parentComponent, taskName + " completed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * override this to handle failure
     * default shows error message
     */
    protected void onFailure(Exception e) {
        Logger.errlog(taskName + " failed: " + e.getMessage(), e);
        JOptionPane.showMessageDialog(parentComponent, taskName + " failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * override this to perform UI updates after completion
     * called regardless of success/failure
     */
    protected void onComplete() {
        // override in subclass if needed
    }

    /**
     * override this to handle cancel from progress dialog
     * called when user clicks Cancel button during retry mode
     */
    protected void onCancel() {
        // override in subclass if needed
    }
    
    /**
     * update progress message (call from performTask)
     */
    protected void updateProgress(String message) {
        publish(message);
    }
    
    /**
     * Attach an existing progress dialog to avoid re-opening / flickering on retry.
     */
    public void attachProgressDialog(ProgressDialog dialog) {
        if (dialog != null) {
            if (this.progressDialog != null && this.progressDialog != dialog) {
                this.progressDialog.dispose();
            }
            this.progressDialog = dialog;
        }
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
        
        // show modal dialog if not already visible
        if (!progressDialog.isVisible()) {
            progressDialog.setVisible(true);
        }
    }

    /**
     * execute the task with retry/cancel support on the progress dialog.
     * on failure, the dialog stays open showing Retry and Cancel buttons
     * instead of closing and showing a JOptionPane.
     */
    public void executeWithRetry() {
        this.retryOnFailure = true;
        executeWithDialog();
    }

    /**
     * get the progress dialog (for external configuration)
     */
    public ProgressDialog getProgressDialog() {
        return progressDialog;
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
        try {
            // get result
            Boolean success = get();
            
            if (success != null && success) {
                progressDialog.closeDialog();
                onSuccess();
                onComplete();
            } else {
                handleFailure(new Exception("Operation returned false"));
            }
            
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            handleFailure(cause instanceof Exception ? (Exception) cause : new Exception(cause));
        }
        
        Logger.log("BackgroundTask", taskName + " finished");
    }

    private void handleFailure(Exception e) {
        if (retryOnFailure) {
            // show retry/cancel on the progress dialog instead of closing
            String errorMsg = e.getMessage() != null && !e.getMessage().equals("Operation returned false")
                    ? e.getMessage()
                    : taskName + " failed. Please check and try again!";
            progressDialog.setMessage(taskName + " Failed");
            progressDialog.showRetryCancel(errorMsg);
            
            // set up retry: reuse existing dialog, create new task, execute it
            progressDialog.setOnRetryAction(() -> {
                BackgroundTask retryTask = createRetryTask();
                retryTask.attachProgressDialog(progressDialog);
                retryTask.executeWithRetry();
            });
            
            // set up cancel: call onCancel and onComplete
            progressDialog.setOnCancelAction(() -> {
                onCancel();
                onComplete();
            });
            // dialog stays open — buttons will handle closing
        } else {
            progressDialog.closeDialog();
            onFailure(e);
            onComplete();
        }
    }

    /**
     * create a new task instance for retry.
     * subclasses must override this when using executeWithRetry().
     */
    protected BackgroundTask createRetryTask() {
        throw new UnsupportedOperationException(
            "createRetryTask() must be overridden when using executeWithRetry()");
    }
}
