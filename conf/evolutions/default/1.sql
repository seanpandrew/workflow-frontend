CREATE TABLE stub (
    pk                 serial          primary key
  , working_title      varchar(128)    not null
  , section            varchar(128)    not null
  , due                timestamp
  , assign_to          varchar(128)
);

CREATE TABLE content (
    pk                 serial          primary key
  , stub               integer         not null         references stub
  , composer_id        varchar(32)     unique
  , path               varchar(128)    not null
  , last_modified      timestamp       not null
  , last_modified_by   varchar(32)
  , status             varchar(16)     not null
  , content_type       varchar(16)
);
