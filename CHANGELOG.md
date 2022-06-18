# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.8.0] - 2022-06-18
- Added supported for more recent IDEs

## [3.7.2] - 2022-03-12
- Fixed issues with state update after auth or repo-sync actions.

## [3.7.1] - 2022-03-11
- Fixed issues with multi window projects and projects opened using attach option.

## [3.7.0] - 2021-12-19
- Added the ability to use access tokens from user.yml file instead of condesync file.
- Some bug fixes and code improvements.

## [3.6.0] - 2021-12-01
- Removed "User Another Account" feature from user actions.
- Fixes related to timezone that were causing strange behavior on diff uploads.
- Fixes for duplicate creation of diff files for the same change.
- Directories like .git, node_modules are ignore by default.

## [3.5.0] - 2021-11-20
- Added more notifications that will give user more information on the status of repo, and give insights if some error happens.
- Added more logging that will make debugging errors easier.
- Added options for repo and file playback. 
- Minor UX improvements related to CodeSync menus.
- Other bug fixes and feature enhancements.


## [3.3.0] - 2021-11-06
- Fixed capitilization issue of a message.
- Added more success and error message to give user more feedback on repo initialization process.
- Fixes for windows path related issues and message improvement.


## [3.2.0] - 2021-10-27
- Fixes for cloud watch logging.
- Fixes for diff handling to make sure files are not processed multiple times.
- Fixes for configuration related issues that were causing plugin issue on startup.

## [3.1.0] - 2021-10-26
- Added support for windows, bug fixes and improvements.

## [3.0.0] - 2021-10-09
- Added authentication flow with CodeSync.
- Non-IDE (external) event handling, plugin detects and handles changes made outside the IDE.
- Added support for windows paths, and added support for plugin on widows.
- Added repo-init functionality right from the plugin.
- Some Bug fixes and features enhancements

## [2.0.0] - 2021-08-08
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

