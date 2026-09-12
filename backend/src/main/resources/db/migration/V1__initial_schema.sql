CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    headline VARCHAR(255),
    bio TEXT,
    location VARCHAR(255),
    years_of_experience NUMERIC(4, 1),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_profiles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_user_profiles_years_of_experience
        CHECK (years_of_experience >= 0)
);

CREATE TABLE skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uq_skills_name UNIQUE (name)
);

CREATE TABLE user_profile_skills (
    user_profile_id UUID NOT NULL,
    skill_id UUID NOT NULL,
    proficiency VARCHAR(30),
    years_experience NUMERIC(4, 1),

    PRIMARY KEY (user_profile_id, skill_id),

    CONSTRAINT fk_user_profile_skills_profile
        FOREIGN KEY (user_profile_id)
        REFERENCES user_profiles(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_profile_skills_skill
        FOREIGN KEY (skill_id)
        REFERENCES skills(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_user_profile_skills_years_experience
        CHECK (years_experience >= 0),

    CONSTRAINT chk_user_profile_skills_proficiency
        CHECK (
            proficiency IS NULL
            OR proficiency IN (
                'BEGINNER',
                'INTERMEDIATE',
                'ADVANCED',
                'EXPERT'
            )
        )
);

CREATE TABLE resumes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_resumes_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE job_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    company VARCHAR(255) NOT NULL,
    job_title VARCHAR(255) NOT NULL,
    job_url VARCHAR(2048),
    job_description TEXT NOT NULL,
    location VARCHAR(255),
    employment_type VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'SAVED',
    applied_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_job_applications_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_job_applications_status
        CHECK (
            status IN (
                'SAVED',
                'APPLIED',
                'OA',
                'INTERVIEW',
                'OFFER',
                'REJECTED',
                'WITHDRAWN'
            )
        )
);

CREATE TABLE job_application_analysis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_application_id UUID NOT NULL,
    resume_id UUID NOT NULL,
    match_score NUMERIC(5, 2),
    strengths TEXT,
    gaps TEXT,
    recommendation TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_analysis_job_application
        FOREIGN KEY (job_application_id)
        REFERENCES job_applications(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_analysis_resume
        FOREIGN KEY (resume_id)
        REFERENCES resumes(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_analysis_match_score
        CHECK (
            match_score IS NULL
            OR (match_score >= 0 AND match_score <= 100)
        )
);

CREATE INDEX idx_user_profiles_user_id
    ON user_profiles(user_id);

CREATE INDEX idx_user_profile_skills_skill_id
    ON user_profile_skills(skill_id);

CREATE INDEX idx_resumes_user_id
    ON resumes(user_id);

CREATE INDEX idx_job_applications_user_id
    ON job_applications(user_id);

CREATE INDEX idx_job_applications_status
    ON job_applications(status);

CREATE INDEX idx_job_application_analysis_application_id
    ON job_application_analysis(job_application_id);

CREATE INDEX idx_job_application_analysis_resume_id
    ON job_application_analysis(resume_id);