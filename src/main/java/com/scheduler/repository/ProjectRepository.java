package com.scheduler.repository;
import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.scheduler.model.Project;

@Repository
public interface ProjectRepository extends CrudRepository<Project, Long>{
	
	List<Project> findByName(String name);

}