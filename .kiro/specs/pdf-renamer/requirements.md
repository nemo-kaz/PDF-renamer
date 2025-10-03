# Requirements Document

## Introduction

PDF リネーミングツールは、指定されたディレクトリとそのサブディレクトリ内のすべての PDF ファイルを再帰的に検索し、各 PDF ファイルのタイトルプロパティに基づいてファイル名を自動的に変更する Groovy CLI コマンドです。このツールは、PDF ファイルの管理を効率化し、ファイル名とコンテンツの一貫性を保つことを目的としています。

## Requirements

### Requirement 1

**User Story:** As a user, I want to recursively scan directories for PDF files, so that I can process all PDF files in a directory tree structure.

#### Acceptance Criteria

1. WHEN the command is executed THEN the system SHALL recursively traverse all subdirectories from the current working directory
2. WHEN entering each directory THEN the system SHALL examine all files in that directory
3. WHEN examining files THEN the system SHALL identify PDF files by their file extension
4. WHEN processing directories THEN the system SHALL create a full path list of all files found

### Requirement 2

**User Story:** As a user, I want to generate a single Groovy script that contains all rename operations, so that I can review and execute the changes in a controlled manner.

#### Acceptance Criteria

1. WHEN the tool starts THEN the system SHALL create a file named "renamePDFs.groovy" in the current directory
2. WHEN processing PDF files THEN the system SHALL append rename commands to the output Groovy file
3. WHEN generating commands THEN the system SHALL use Files.move() method for renaming operations
4. WHEN writing file paths THEN the system SHALL enclose paths in triple quotes (""") to prevent errors

### Requirement 3

**User Story:** As a user, I want to rename PDF files based on their title property, so that file names reflect the actual content of the documents.

#### Acceptance Criteria

1. WHEN processing a PDF file THEN the system SHALL read the title property from the PDF metadata
2. WHEN the title property is valid THEN the system SHALL generate a rename command using the title as the new filename
3. WHEN generating the new filename THEN the system SHALL append ".pdf" extension to the title
4. WHEN the rename operation is generated THEN the system SHALL include a println statement showing the old and new paths

### Requirement 4

**User Story:** As a user, I want certain PDF files to be excluded from renaming, so that files with specific naming patterns remain unchanged.

#### Acceptance Criteria

1. WHEN the title starts with "_" THEN the system SHALL NOT generate a rename command
2. WHEN the title starts with "~" THEN the system SHALL NOT generate a rename command  
3. WHEN the title starts with "Untitled" THEN the system SHALL NOT generate a rename command
4. WHEN the title is an empty string THEN the system SHALL NOT generate a rename command

### Requirement 5

**User Story:** As a user, I want invalid Windows filename characters to be replaced, so that the generated filenames are compatible with the Windows file system.

#### Acceptance Criteria

1. WHEN the title contains ":" THEN the system SHALL replace it with "_"
2. WHEN the title contains '"' THEN the system SHALL replace it with "_"
3. WHEN the title contains other Windows-invalid characters THEN the system SHALL replace them with "_"
4. WHEN sanitizing filenames THEN the system SHALL ensure the resulting filename is valid for Windows file system

### Requirement 6

**User Story:** As a user, I want to see the rename operations in a specific format, so that I can understand what changes will be made.

#### Acceptance Criteria

1. WHEN generating rename commands THEN the system SHALL use Paths.get() to create path objects
2. WHEN creating path objects THEN the system SHALL use forward slashes in path strings
3. WHEN generating output THEN the system SHALL follow the format: def oldPath = Paths.get("path"), def newPath = Paths.get("path"), Files.move(oldPath, newPath)
4. WHEN executing rename operations THEN the system SHALL print "リネーム: ${oldPath} -> ${newPath}"