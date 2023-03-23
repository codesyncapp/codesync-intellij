# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.20.0] - 2023-03-23
- Added support for latest intelliJ IDE version.
- Added fixes for dialog related issues on some systems.

## [3.19.4] - 2023-03-17
- Added fixes for alerts logic.

## [3.19.3] - 2023-02-22
- Added more logging to debug issues that users are facing.

## [3.19.2] - 2023-02-22
- Fixed a bug that was causing suppression on dialogs.

## [3.19.1] - 2023-02-22
- Fixed a bug that was causing failure in the detection of new line characters.
- Removed changes handler logic from main thread into a separate thread

## [3.18.5] - 2022-01-28
- Fixed date time formatting issues for alerts file.
- Fixed a logic issue against activity data returned by server.

## [3.18.4] - 2022-01-27
- Added delay for invalid request to get activity data.
- Refactored the code a bit.

## [3.18.3] - 2022-01-26
- Fixed file path issue for lock files.
- Updates for the daemon calling functionality.
- Fixed a path issue in the project lock file for Windows OS.
- Updated logic for alerts to handle an edge case.
- If previous alert was shown before 4 PM then we need to show another one at 4:30 PM

## [3.18.2] - 2022-01-20
- Fixed a quick bug observed by new users.

## [3.18.1] - 2022-01-15
- Bug fixes related to daemon processes and yml files.
- Refactored and improved locking logic to handle edge cases.
- Fixed a bug with progress indicator, it should work correctly now.
- Fixed a bug with activity alerts that was blocking alerts if user clicked on remind later.

## [3.18.0] - 2022-01-11
- Fixes and improvements in the plugin.
- Show login prompt before repo-sync prompt.
- Added handling and guards agains null values of Project instance in different places inside the code base. This will help avoid null pointer exceptions.
- Explicitly set API timeout to 120 seconds for requests, also refactored client related code to add better error handling and make it cleaner.
- Included `added_at` parameter for new files so that correct time is used if user creates a file during the time period when user's plan has expired.
- Added daily reminder alert for team activity review.
- Started using ScheduledExecutorService instead of Timer to schedule async processes.
- In case user has exhausted his plan limit we should show dialog in user tries to connect a new repo using actions.
-  Added alerts.yml file for syncing with VSCode.

## [3.17.1] - 2022-12-27
- Improved pricing alert dialog box.
- Improved error handling during repo init flow.
- Added IDE name in logging and GA4 tags for better understanding of errors.
- Added analytics tag for websocket connections.
- Fixed new file creation timestamp in diff files.

## [3.17.0] - 2022-12-24
- Added dialog box for a better user experience and enabled a way to start free trial if user is eligible. 

## [3.16.0] - 2022-12-20
- Added handling for stale diff files belonging to a branch that was not synced.

## [3.15.0] - 2022-12-16
- Added pricing alert feature for intelliJ users.

## [3.14.0] - 2022-10-25
- Added support for the latest IDE.

## [3.13.2] - 2022-09-27
- Fixed an issue related to change detection logic.

## [3.13.1] - 2022-09-16
- Fixed issues related to setup action from the menu.

## [3.12.0] - 2022-09-16
- Added logging capability for users who have not authenticated yet, this will help with the debugging og authenticated relates issues.

## [3.12.0] - 2022-09-16
- Fixes for windows related issues, and other bugs.
- Fixed auth server issues that should fix the login issue.

## [3.11.1] - 2022-09-06
- Fixed compatibility issue for IDEs older than March 2020

## [3.11.0] - 2022-09-04
- Add platform name in the CW log event.
- Updated handling for notifications and deprecated the old API to start using the new one.
- Added CodeSync icon on user input dialogs.
- We should not handle updates for projects that disposed.
- Updated wording of notifications to correctly indicate repo or branch that is being synced.

## [3.10.3] - 2022-09-02
- Improved logging for the plugin errors.

## [3.10.2] - 2022-09-02
- Reduced thread count for authentication server down to 1.

## [3.10.1] - 2022-08-31
- Improved error logs and added more logging for debugging issues with the authentication flow.

## [3.10.0] - 2022-08-30
- Added handling for null file ids.
- A small UX improvement for syncing new branches.

## [3.9.3] - 2022-08-28
- Fixed bug for disconnect action, user can now disconnect repos.
- Updated logic to have single module projects be marked as the default projects.

## [3.9.2] - 2022-08-26
- Bug fixes for populate buffer daemon.

## [3.9.1] - 2022-08-24
- Fix for a bug in synchronisation logic.

## [3.9.0] - 2022-08-22
- Added synchronization logic across IntelliJ and other IDEs running codesync.

## [3.8.12] - 2022-08-16
- Server URLs updated

## [3.8.11] - 2022-08-07
- Bug fix related to path normalisation on Windows OS.

## [3.8.10] - 2022-07-27
- Cosmetic changes and better handling for authentication related actions.

## [3.8.9] - 2022-07-27
- Handle server failure gracefully

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

