# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
- Daemon with basic functionality of checking server availability, validating and grouping diffs
- Diffs are being uploaded to server via websocket
- Docs added for handleBuffer, Fixed order of uploading diffs after authentication
- utils/buffer_utils.ts added
- is_dir_rename & is_rename diffs handled, using walk package for os.walk
- Implemented New File Upload to server & s3, new package added isbinaryfile
- put_log_events replicated using aws-sdk
- Directory delete handled

## [1.3.0] - 2021-05-07
### Changed
- Updating readme for tremendous enjoyment and superior marketing
- Updated svg icon
- Until build value updated to 211.*
- Updated plugin.yml with README content
- Common function added to manage diffs

## [1.2.0] - 2021-04-30
### Added
- Added basic strcuture and jar file for diff-match-patch
- Getting git branch name on run time using bash command
- Handling events for File Create/Update/Rename/Delete
- Directory level diffs implemented
- Skipping events if directory is not in sync
- Ignoring .git repo to be synced for all events
- Directory rename has been handled
- Skipping directory events for New/Deleted events
- DirRename diff introduced to manage nested renames from daemon side
- README Updated
- Added icon
- Updated gradle version to v6.8.3

### Fixed
- Fixed duplication for FileDeleted events

