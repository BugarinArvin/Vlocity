package com.scheduler.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "task")
public class Task {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column
	private Long id;

	@Column
	private String name;

	@Column
	private int durationInDays;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id")
	private Project project;

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "task_id")
	private Task task;

	@OneToMany(mappedBy = "task")
	private Set<Task> dependencies = new HashSet<Task>();

	public Task() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getDurationInDays() {
		return durationInDays;
	}

	public void setDurationInDays(int durationInDays) {
		this.durationInDays = durationInDays;
	}

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
	}

	public Set<Task> getDependencies() {
		return dependencies;
	}

	public void setDependencies(Set<Task> dependencies) {
		this.dependencies = dependencies;
	}

	@Override
	public String toString() {
		return String.format("Task [id=%s, name=%s, , durationInDays=%s, dependencies=%s]", id, name, durationInDays,
				dependencies);
	}

//	@Override
//	public boolean equals(Object o) {
//		Task task = (Task) o;
//		if (task.getName() == this.getName()) {
//
//			return true;
//		}
//		return false;
//	}

//	@Override
//	public int hashCode() {
//		return (int)((long)this.getId());
//	}

}