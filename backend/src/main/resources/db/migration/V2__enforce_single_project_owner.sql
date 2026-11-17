CREATE UNIQUE INDEX uq_project_members_single_owner
    ON project_members (project_id)
    WHERE project_role = 'OWNER';
