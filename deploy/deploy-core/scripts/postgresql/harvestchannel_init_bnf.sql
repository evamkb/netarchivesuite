-- $Id: jobcategories_init_bnf.sql 1414 2010-05-31 15:52:06Z ngiraud $
-- $Revision: 1414 $
-- $Date: 2010-05-31 17:52:06 +0200 (Mon, 31 May 2010) $
-- $Author: ngiraud $

-- PostgreSQL initialization script
-- presupposes PostgresSQL 8.3+
-- tested on PostgreSQL 8.4.1-1 (Ubuntu 9.10)

-- Initializes the harvestchannel table to values customized for BnF.

--
-- How to use:
-- psql -U <user name> -W [DB name] < jobcategories_init_bnf.sql

SET search_path TO netarchivesuite;

INSERT INTO harvestchannel(name, isdefault, issnapshot, comments) 
    VALUES ('CIBLEE', true, false, 'Canal par défaut pour les collectes ciblées');
INSERT INTO harvestchannel(name, isdefault, issnapshot , comments) 
    VALUES ('LARGE', true, true, 'Canal par défaut pour les collectes larges');