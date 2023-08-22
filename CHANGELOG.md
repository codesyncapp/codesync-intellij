# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.23.4] - 2023-08-22
- Gracefully handling error 400 which is being returned when file is in syncignore.
- Updated playback gif on plugin.xml file for plugin page.
- Removing log related to skipping event due to file is ignored.

## [3.23.3] - 2023-08-01
- Removed sequence token file and sequence token from the payload to cloud watch API calls.
- Added dependabot.yml file to automate update dependencies update.
- Added SECURITY.md file to give instruction to people on how to report vulnerabilities.

## [3.23.2] - 2023-07-29
- Updated unitBuild value so plugin can run on latest Intellij version.

## [3.23.1] - 2023-07-25
- Fixed writeEmptyDictToFile method issue in by closing the lock after writing so file can be flushed and closed.
- Put check for LockFile expiry getting null and text getting null while reading YML file.
- put check for fileContent variable if it is null and send logs to cloud watch for that file.

## [3.23.0] - 2023-07-14
- Implemented Mocking for testing static method calls in getOS and sendPost methods.
- Updated Git workflows to build and test on all three operating systems Windows, MacOS and Linux
- Update playback picture on GitHub repository in README.md file.
- Fixed websocket was getting null when server was restarting.

## [3.22.2] - 2023-06-20
- Put check for batch size of diffs being sent in a request should not exceed 16mb.
- Implemented Singleton design pattern for SQLite connection which fixed the bug of connection getting closed when switching between projects or multiple projects opened.

## [3.22.1] - 2023-06-07
- Fixed diffs not being sent to server after getting online when synced projects has started without internet.
- Fixed a bug that was causing offline changes to have incorrect associated time.

## [3.22.0] - 2023-05-25
- Removed UserPlan and User class and their usages.
- Fixed synced project not opening when internet connection is not available.
- Migrated to SQLite implementation for User info from user.yml files.

## [3.21.0] - 2023-04-25
- Disable Connect Repo button when repository is in process of being connected and uploaded.
- Skip Healthcheck Test and when server is down shows connection error in bufferHandler catch block.
- Don't send user data on post request in Authenticator as token is already provided.

## [3.20.3] - 2023-04-21
- Added tests for config file updates, updated logic so that async writes and locking wait do not cause the loss of data added in previous writes.
- Added more error handling for cloud watch logs so that it gracefully handles errors and does not interrupt users.

## [3.20.2] - 2023-04-14
- Added synchronization logic for writes to config files, a locking mechanism would allow multiple threads write to the config file.
- Added handling for invalid config files, config file will be emptied if its contents are not valid.
- Changes for inactive repositories would be ignored now, previously diff files were being created causing buffer size increase.
- Offline changes for inactive repositories will be ignored.
- Updated logic to use correct initial values for fields such as `isDisconnected`, `pauseNotification` and `isDeleted` for newly synced repos.

## [3.20.1] - 2023-04-09
-  Fixed handling for multi-module projects in languages that support modules such as java. These were being considered as separate repo by the plugin.
- Added fixes for sync prompt at the start, it was incorrectly prompting user to sync the repo if user creates a new branch offline for an already synced repo.
- Fixed invalid payload issue for branch syncing process, specifically `is_public` was missing and causing an error.
- Improved messaging of repo in sync message.

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

