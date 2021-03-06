package com.scheduler.repository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.scheduler.model.Task;

@Repository
public interface TaskRepository extends CrudRepository<Task, Long>{

}