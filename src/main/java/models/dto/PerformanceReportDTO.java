/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Performance Report (REP64)
 * Contains worker productivity metrics, task breakdowns, summary KPIs, and efficiency trends
 *
 * @author Sanod
 */
public class PerformanceReportDTO {

    private String generatedAt;
    private String fromDate;
    private String toDate;
    private int totalTasksCompleted;
    private double averageTasksPerHour;
    private double overallAccuracyRate;
    private String averageTimePerTask = "0.0 min";
    private double errorRate;

    private List<WorkerPerformanceItem> workers = new ArrayList<>();
    private List<TaskBreakdownItem> taskBreakdowns = new ArrayList<>();
    private List<PerformanceTrendItem> trends = new ArrayList<>();

    public PerformanceReportDTO() {
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    public int getTotalTasksCompleted() {
        return totalTasksCompleted;
    }

    public void setTotalTasksCompleted(int totalTasksCompleted) {
        this.totalTasksCompleted = totalTasksCompleted;
    }

    public double getAverageTasksPerHour() {
        return averageTasksPerHour;
    }

    public void setAverageTasksPerHour(double averageTasksPerHour) {
        this.averageTasksPerHour = averageTasksPerHour;
    }

    public double getOverallAccuracyRate() {
        return overallAccuracyRate;
    }

    public void setOverallAccuracyRate(double overallAccuracyRate) {
        this.overallAccuracyRate = overallAccuracyRate;
    }

    public String getAverageTimePerTask() {
        return averageTimePerTask;
    }

    public void setAverageTimePerTask(String averageTimePerTask) {
        this.averageTimePerTask = averageTimePerTask;
    }

    public double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(double errorRate) {
        this.errorRate = errorRate;
    }

    public List<WorkerPerformanceItem> getWorkers() {
        return workers;
    }

    public void setWorkers(List<WorkerPerformanceItem> workers) {
        this.workers = workers;
    }

    public List<TaskBreakdownItem> getTaskBreakdowns() {
        return taskBreakdowns;
    }

    public void setTaskBreakdowns(List<TaskBreakdownItem> taskBreakdowns) {
        this.taskBreakdowns = taskBreakdowns;
    }

    public List<PerformanceTrendItem> getTrends() {
        return trends;
    }

    public void setTrends(List<PerformanceTrendItem> trends) {
        this.trends = trends;
    }

    /**
     * Item model for worker performance table
     */
    public static class WorkerPerformanceItem {
        private String username;
        private String workerName;
        private int tasksCompleted;
        private double tasksPerHour;
        private double accuracyPct;
        private String avgTimePerTask = "0.0 min";
        private int errors;
        private String rating = "Good";

        public WorkerPerformanceItem() {
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getWorkerName() {
            return workerName;
        }

        public void setWorkerName(String workerName) {
            this.workerName = workerName;
        }

        public int getTasksCompleted() {
            return tasksCompleted;
        }

        public void setTasksCompleted(int tasksCompleted) {
            this.tasksCompleted = tasksCompleted;
        }

        public double getTasksPerHour() {
            return tasksPerHour;
        }

        public void setTasksPerHour(double tasksPerHour) {
            this.tasksPerHour = tasksPerHour;
        }

        public double getAccuracyPct() {
            return accuracyPct;
        }

        public void setAccuracyPct(double accuracyPct) {
            this.accuracyPct = accuracyPct;
        }

        public String getAvgTimePerTask() {
            return avgTimePerTask;
        }

        public void setAvgTimePerTask(String avgTimePerTask) {
            this.avgTimePerTask = avgTimePerTask;
        }

        public int getErrors() {
            return errors;
        }

        public void setErrors(int errors) {
            this.errors = errors;
        }

        public String getRating() {
            return rating;
        }

        public void setRating(String rating) {
            this.rating = rating;
        }
    }

    /**
     * Item model for task breakdown table
     */
    public static class TaskBreakdownItem {
        private String taskType;
        private int count;
        private String avgTime = "0.0 min";
        private double accuracyPct;
        private double totalQtyHandled;

        public TaskBreakdownItem() {
        }

        public String getTaskType() {
            return taskType;
        }

        public void setTaskType(String taskType) {
            this.taskType = taskType;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public String getAvgTime() {
            return avgTime;
        }

        public void setAvgTime(String avgTime) {
            this.avgTime = avgTime;
        }

        public double getAccuracyPct() {
            return accuracyPct;
        }

        public void setAccuracyPct(double accuracyPct) {
            this.accuracyPct = accuracyPct;
        }

        public double getTotalQtyHandled() {
            return totalQtyHandled;
        }

        public void setTotalQtyHandled(double totalQtyHandled) {
            this.totalQtyHandled = totalQtyHandled;
        }
    }

    /**
     * Item model for efficiency trends (line chart)
     */
    public static class PerformanceTrendItem {
        private String date;
        private int tasksCompleted;
        private double tasksPerHour;

        public PerformanceTrendItem() {
        }

        public PerformanceTrendItem(String date, int tasksCompleted, double tasksPerHour) {
            this.date = date;
            this.tasksCompleted = tasksCompleted;
            this.tasksPerHour = tasksPerHour;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public int getTasksCompleted() {
            return tasksCompleted;
        }

        public void setTasksCompleted(int tasksCompleted) {
            this.tasksCompleted = tasksCompleted;
        }

        public double getTasksPerHour() {
            return tasksPerHour;
        }

        public void setTasksPerHour(double tasksPerHour) {
            this.tasksPerHour = tasksPerHour;
        }
    }
}
