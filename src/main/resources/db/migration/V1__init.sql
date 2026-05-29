-- 정책: 외래키 제약은 DB 레벨에 두지 않는다. 참조 무결성은 JPA/서비스 레이어에서 관리.
-- 조회 성능 보장을 위해 FK 컬럼에는 인덱스를 유지한다.

CREATE TABLE admins (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    key_hash    VARCHAR(64)  NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE adoptions (
    id              BIGSERIAL PRIMARY KEY,
    admin_id        BIGINT       NOT NULL,
    name            VARCHAR(200) NOT NULL,
    goal            TEXT         NOT NULL,
    target_audience TEXT         NOT NULL,
    concern         TEXT,
    target_count    INTEGER      NOT NULL CHECK (target_count > 0),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_adoptions_admin ON adoptions(admin_id);

CREATE TABLE surveys (
    id                  BIGSERIAL PRIMARY KEY,
    adoption_id         BIGINT       NOT NULL,
    title               VARCHAR(200) NOT NULL,
    public_slug         VARCHAR(64)  NOT NULL UNIQUE,
    deadline            TIMESTAMPTZ  NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    published_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_surveys_adoption ON surveys(adoption_id);

CREATE TABLE questions (
    id           BIGSERIAL PRIMARY KEY,
    survey_id    BIGINT       NOT NULL,
    type         VARCHAR(20)  NOT NULL,
    text         TEXT         NOT NULL,
    order_index  INTEGER      NOT NULL,
    required     BOOLEAN      NOT NULL DEFAULT TRUE,
    axis         VARCHAR(20)
);
CREATE INDEX idx_questions_survey ON questions(survey_id, order_index);

CREATE TABLE question_options (
    id           BIGSERIAL PRIMARY KEY,
    question_id  BIGINT       NOT NULL,
    text         VARCHAR(300) NOT NULL,
    order_index  INTEGER      NOT NULL
);
CREATE INDEX idx_options_question ON question_options(question_id, order_index);

CREATE TABLE survey_responses (
    id            BIGSERIAL PRIMARY KEY,
    survey_id     BIGINT       NOT NULL,
    access_token  VARCHAR(64)  NOT NULL UNIQUE,
    status        VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS',
    submitted_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_responses_survey ON survey_responses(survey_id);

CREATE TABLE answers (
    id                  BIGSERIAL PRIMARY KEY,
    survey_response_id  BIGINT  NOT NULL,
    question_id         BIGINT  NOT NULL,
    text_value          TEXT,
    question_option_id  BIGINT,
    scale_value         INTEGER,
    CHECK (
        (text_value IS NOT NULL)::int +
        (question_option_id IS NOT NULL)::int +
        (scale_value IS NOT NULL)::int = 1
    )
);
CREATE INDEX idx_answers_response ON answers(survey_response_id);
CREATE INDEX idx_answers_question ON answers(question_id);

CREATE TABLE analyses (
    id                  BIGSERIAL PRIMARY KEY,
    survey_id           BIGINT      NOT NULL,
    adoption_score      INTEGER     NOT NULL,
    usage_score         INTEGER     NOT NULL,
    behavior_score      INTEGER     NOT NULL,
    value_score         INTEGER     NOT NULL,
    positive_signals    JSONB       NOT NULL,
    resistance_factors  JSONB       NOT NULL,
    risks               JSONB       NOT NULL,
    raw_output          TEXT        NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_analyses_survey ON analyses(survey_id, created_at DESC);

CREATE TABLE action_items (
    id           BIGSERIAL PRIMARY KEY,
    adoption_id  BIGINT       NOT NULL,
    analysis_id  BIGINT       NOT NULL,
    title        VARCHAR(300) NOT NULL,
    description  TEXT,
    priority     VARCHAR(10)  NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'TODO',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_action_items_adoption ON action_items(adoption_id);
