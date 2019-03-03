package com.scheduler.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.scheduler.model.Project;
import com.scheduler.model.Task;
import com.scheduler.repository.ProjectRepository;
import com.scheduler.repository.TaskRepository;

import de.vandermeer.asciitable.AT_Row;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_LongestLine;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;

/**
 * @author Arvin Bugarin
 *
 */
@Service
public class SchedulerServiceImpl implements SchedulerService {

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private TaskRepository taskRepository;

	Logger logger = LoggerFactory.getLogger(SchedulerServiceImpl.class);

	public void startScheduler() {
		askUser();
	}

	/**
	 * ask user for action, either add a project plan or print a project schedule
	 */
	private void askUser() {
		Scanner scanner = new Scanner(System.in);

		List<Project> projects = (List<Project>) projectRepository.findAll();
		logger.info("Projects {}", projects);

		if (projects.size() > 0) {
			printProjectList(projects);
			ask("Do you want to ADD a project or PRINT a project schedule (type add or print only)?");
			String action = scanner.nextLine().trim();
			if (action.equalsIgnoreCase("print")) {
				printProjectPlanSchedule(scanner);
			} else if (action.equalsIgnoreCase("add")) {
				addProject(scanner);
			} else {
				exitScheduler(scanner);
			}
		}
		addProject(scanner);
	}

	/**
	 * @param scanner close scanner and exit
	 */
	private void exitScheduler(Scanner scanner) {
		scanner.close();
		System.exit(0);
	}

	/**
	 * @param scanner 
	 * print schedule of tasks in console
	 */
	private void printProjectPlanSchedule(Scanner scanner) {
		ask("Please enter \"PROJECT ID\" of project to print:");
		Project projectToPrint = projectRepository.findById(Long.valueOf(scanner.nextLine().trim())).orElse(null);

		AsciiTable at = new AsciiTable();
		at.getRenderer().setCWC(new CWC_LongestLine());
		AT_Row row;
		at.addRule();
		row = at.addRow("TASK NAME", "START DATE", "END DATE");
		row.getCells().get(0).getContext().setTextAlignment(TextAlignment.CENTER);
		row.getCells().get(1).getContext().setTextAlignment(TextAlignment.CENTER);
		row.getCells().get(2).getContext().setTextAlignment(TextAlignment.CENTER);

		Map<Task, LocalDate> taskMap = new HashMap<>();
		Set<Task> tasksEdit = new HashSet<Task>(projectToPrint.getTasks());

		while (tasksEdit.size() > 0) {
			List<Task> tasksDelete = new ArrayList<>();
			LocalDate startDate = LocalDate.now().plusDays(1);

			for (Task task : tasksEdit) {
				if (task.getDependencies().size() == 0) {
					tasksDelete.add(task);
					taskMap.putIfAbsent(task, startDate);
				}
			}

			for (Task task : tasksEdit) {
				for (Task taskDelete : tasksDelete) {
					if (task.getDependencies().contains(taskDelete)) {
						taskMap.merge(task, taskMap.get(taskDelete).plusDays(taskDelete.getDurationInDays()),
								(v1, v2) -> {
									return v1.isAfter(v2) ? v1 : v2;
								});
					}
				}
				tasksDelete.forEach(t -> task.getDependencies().remove(t));
			}
			tasksDelete.forEach(t -> tasksEdit.remove(t));

		}

		logger.info("taskmap {}", taskMap.toString());

		Map<Task, LocalDate> sortedTaskMap = sortHashMapbyValue(taskMap);
		sortedTaskMap.forEach((taskKey, taskvalue) -> {
			at.addRule();
			AT_Row roww;
			roww = at.addRow(taskKey.getName(), taskvalue, taskvalue.plusDays(taskKey.getDurationInDays()));
			roww.getCells().get(0).getContext().setTextAlignment(TextAlignment.CENTER);
			roww.getCells().get(1).getContext().setTextAlignment(TextAlignment.CENTER);
			roww.getCells().get(2).getContext().setTextAlignment(TextAlignment.CENTER);

		});

		at.addRule();
		System.out.println();
		System.out.printf("PROJECT PLAN : %s%n", projectToPrint.getName());
		System.out.printf("PROJECT START DATE: %s%n", LocalDate.now().plusDays(1));
		System.out.println("SCHEDULE OF TASK");
		System.out.println(at.render());

		askUserIfContinue(scanner);
	}

	/**
	 * @param taskMap
	 * @return sorted task map by start date
	 */
	private LinkedHashMap<Task, LocalDate> sortHashMapbyValue(Map<Task, LocalDate> taskMap) {
		return taskMap.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors
				.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
	}

	/**
	 * @param scanner exit or continue scheduler
	 */
	private void askUserIfContinue(Scanner scanner) {
		ask("Do you still want to continue (y or n)?");

		if (scanner.nextLine().trim().equalsIgnoreCase("n")) {
			exitScheduler(scanner);
		} else {
			askUser();
		}
	}

	/**
	 * @param scanner 
	 * Get project and task details and save to H2 Database
	 */
	private void addProject(Scanner scanner) {
		String projectName = null;
		boolean taskDataEntry = true;
		Project project = new Project();

		ask("What's the name of your project plan?");
		projectName = scanner.nextLine().trim();
		project.setName(projectName);

		while (taskDataEntry) {
			taskDataEntry = addTask(scanner, projectName, taskDataEntry, project);
		}
		project = projectRepository.findByName(projectName).stream().findFirst().orElse(null);
		if (project != null) {
			addTaskDependencies(scanner, projectName, project);
		}
		askUser();
	}

	/**
	 * @param scanner
	 * @param projectName
	 * @param project     
	 * Gets task dependencies and save to H2 Database
	 */
	private void addTaskDependencies(Scanner scanner, String projectName, Project project) {
		List<Task> tasks = project.getTasks().stream().sorted((o1, o2) -> o1.getId().compareTo(o2.getId()))
				.collect(Collectors.toList());
		if (tasks.size() > 1) {
			for (Task task : tasks) {
				Project projectInEdit = projectRepository.findByName(projectName).stream().findFirst().orElse(null);
				List<Task> tasksInEdit = projectInEdit.getTasks().stream()
						.filter(o -> o.getName() != task.getName() && !o.getDependencies().contains(task))
						.sorted((o1, o2) -> o1.getId().compareTo(o2.getId())).collect(Collectors.toList());
				if (tasksInEdit.size() == 0) {
					continue;
				}
				ask("Does \"%s\" have dependencies with another task (y or n)?", task.getName());
				if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
					AsciiTable at = new AsciiTable();
					at.getRenderer().setCWC(new CWC_LongestLine());
					AT_Row row;
					at.addRule();
					row = at.addRow("TASK ID", "TASK NAME");
					row.getCells().get(0).getContext().setTextAlignment(TextAlignment.CENTER);
					row.getCells().get(1).getContext().setTextAlignment(TextAlignment.CENTER);

					for (Task taskInEdit : tasksInEdit) {
						at.addRule();
						row = at.addRow(taskInEdit.getId(), taskInEdit.getName());
						row.getCells().get(0).getContext().setTextAlignment(TextAlignment.CENTER);

					}
					at.addRule();
					System.out.println();
					System.out.printf("PROJECT PLAN: %s%n", projectName);
					System.out.printf("LIST OF POSSIBLE TASK DEPENDENCIES FOR \"%s\"%n", task.getName());
					System.out.println(at.render());
					ask("Enter the TASK ID/TASK ID's of \"%s\"'s dependency/dependencies (comma separated if more than one):",
							task.getName());
					String[] taskIdsToAdd = scanner.nextLine().split(",");
					for (String id : taskIdsToAdd) {

						Task taskObjToAdd = taskRepository.findById(Long.valueOf(id.trim())).orElse(null);
						if (taskObjToAdd != null) {
							task.getDependencies().add(taskObjToAdd);
							projectRepository.save(projectInEdit);
							logger.info("Project saved: {}", projectInEdit);
						}
					}

				}
			}
			System.out.println();
			ask("Your Project Details has been successfully saved.");
		}
	}

	/**
	 * @param scanner
	 * @param projectName
	 * @param taskDataEntry
	 * @param project
	 * @return true or false for adding tasks to project
	 */
	private boolean addTask(Scanner scanner, String projectName, boolean taskDataEntry, Project project) {
		String taskName;
		int taskDuration;
		ask("Please enter a task for your \"%s\" project :", projectName);
		taskName = scanner.nextLine().trim();

		ask("What is the duration of \"%s\" task in days (number only)?", taskName);
		taskDuration = Integer.parseInt(scanner.nextLine().trim());

		Task task = new Task();
		task.setName(taskName);
		task.setDurationInDays(taskDuration);
		project.getTasks().add(task);
		projectRepository.save(project);

		ask("Do you want to add more tasks to \"%s\" project (y or n)?", projectName);
		if (scanner.nextLine().trim().equalsIgnoreCase("n")) {
			taskDataEntry = false;
		}
		return taskDataEntry;
	}

	/**
	 * @param projects 
	 * Prints list of projects in database
	 */
	private void printProjectList(List<Project> projects) {
		AsciiTable at = new AsciiTable();
		at.getRenderer().setCWC(new CWC_LongestLine());
		AT_Row row;
		at.addRule();
		row = at.addRow("PROJECT ID", "PROJECT NAME");
		row.getCells().get(0).getContext().setTextAlignment(TextAlignment.CENTER);
		row.getCells().get(1).getContext().setTextAlignment(TextAlignment.CENTER);

		for (Project projectToDisplay : projects) {
			at.addRule();
			row = at.addRow(projectToDisplay.getId(), projectToDisplay.getName());
			row.getCells().get(0).getContext().setTextAlignment(TextAlignment.CENTER);
		}
		at.addRule();
		System.out.println();
		System.out.println();
		System.out.println("LIST OF PROJECT PLANS:");
		System.out.println(at.render());
	}

	private void ask(String question) {
		System.out.print(question + " ");
	}

	private void ask(String question, String arg) {
		System.out.printf(question + " ", arg);
	}
}
