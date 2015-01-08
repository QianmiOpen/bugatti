rename table task_template to template_action;

rename table task_template_step to template_action_step;

ALTER TABLE `template_action_step` ADD COLUMN `do_if` VARCHAR(2000) NULL COMMENT 'do if 表达式';