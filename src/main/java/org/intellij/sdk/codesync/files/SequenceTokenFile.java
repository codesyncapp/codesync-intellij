package org.intellij.sdk.codesync.files;

import org.intellij.sdk.codesync.exceptions.FileLockedError;
import org.intellij.sdk.codesync.exceptions.FileNotCreatedError;
import org.intellij.sdk.codesync.exceptions.InvalidConfigFileError;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;


public class SequenceTokenFile extends CodeSyncYmlFile {
    File sequenceTokenFile;
    Map<String, Object> contentsMap;
    Map<String, SequenceToken> sequenceTokens = new HashMap<>();

    public static class SequenceToken {
        String userEmail, tokenString;

        public SequenceToken(String userEmail, String tokenString) {
            this.userEmail = userEmail;
            this.tokenString = tokenString;
        }

        public String getUserEmail () {
            return this.userEmail;
        }

        public String getTokenString () {
            return this.tokenString;
        }

        public String getYMLAsHashMap() {
            return getTokenString();
        }
    }

    public SequenceTokenFile (String filePath) throws FileNotFoundException, InvalidYmlFileError {
        File sequenceTokenFile = new File(filePath);

        if (!sequenceTokenFile.isFile()) {
            throw new FileNotFoundException(String.format("Sequence token file \"%s\" does not exist.", filePath));
        }
        this.sequenceTokenFile = sequenceTokenFile;
        this.contentsMap = this.readYml();
        this.loadYmlContent();
    }

    public SequenceTokenFile (String filePath, boolean shouldCreateIfAbsent) throws FileNotFoundException, InvalidYmlFileError, FileNotCreatedError {
        File sequenceTokenFile = new File(filePath);

        if (!sequenceTokenFile.isFile() && shouldCreateIfAbsent) {
            boolean isFileReady = createFile(filePath);
            if (!isFileReady) {
                throw new FileNotCreatedError(String.format("User file \"%s\" could not be created.", filePath));
            }
        } else if (!sequenceTokenFile.isFile()) {
            throw new FileNotFoundException(String.format("Sequence token file \"%s\" does not exist.", filePath));
        }
        this.sequenceTokenFile = sequenceTokenFile;
        this.contentsMap = this.readYml();
        this.loadYmlContent();
    }

    public File getYmlFile() {
        return this.sequenceTokenFile;
    }

    public Map<String, Object> getYMLAsHashMap() {
        Map<String, Object> contents = new HashMap<>();
        for (SequenceToken sequenceToken: this.sequenceTokens.values()) {
            contents.put(sequenceToken.getUserEmail(), sequenceToken.getYMLAsHashMap());
        }
        return contents;
    }

    private void loadYmlContent () throws InvalidYmlFileError {
        if (this.contentsMap == null) {
            return;
        }

        try {
            for (Map.Entry<String, Object> userEntry : this.contentsMap.entrySet()) {
                if (userEntry.getValue() != null) {
                    String sequenceToken = (String) userEntry.getValue();
                    this.sequenceTokens.put(userEntry.getKey(), new SequenceToken(userEntry.getKey(), sequenceToken));
                }

            }
        } catch (ClassCastException e){
            throw new InvalidYmlFileError(String.format("Sequence token yml file \"%s\" is not valid.", this.getYmlFile().getPath()));
        }
    }

    public SequenceToken getSequenceToken (String userEmail) {
        return this.sequenceTokens.getOrDefault(userEmail, null);
    }

    public void updateSequenceToken (String userEmail, String sequenceToken) {
        this.sequenceTokens.put(userEmail, new SequenceToken(userEmail, sequenceToken));
    }

    public void publishNewSequenceToken (String userEmail, String sequenceToken) {
        this.updateSequenceToken(userEmail, sequenceToken);
        try {
            this.writeYml();
        } catch (FileNotFoundException | InvalidYmlFileError | FileLockedError e) {
            // Ignore errors while updating to sequence token file
        }
    }

}
