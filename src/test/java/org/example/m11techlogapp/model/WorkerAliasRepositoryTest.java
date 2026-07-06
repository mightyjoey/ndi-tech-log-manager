package org.example.m11techlogapp.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerAliasRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void addWorkerAliasPersistsAliasAndReturnsSuccess() {
        WorkerAliasRepository repository = repositoryForTempDatabase();

        String result = repository.addWorkerAlias("JANE DOE", "J. DOE");

        assertEquals("Worker alias added successfully.", result);
        assertEquals(List.of("J. DOE"), repository.getAliasesForWorker("JANE DOE"));
    }

    @Test
    void addWorkerAliasRejectsDuplicateAliasForSameWorker() {
        WorkerAliasRepository repository = repositoryForTempDatabase();
        repository.addWorkerAlias("JANE DOE", "J. DOE");

        String result = repository.addWorkerAlias("JANE DOE", "J. DOE");

        assertTrue(result.startsWith("ERROR:"), "expected error but was: " + result);
        assertEquals(1, repository.getAliasesForWorker("JANE DOE").size());
    }

    @Test
    void getAliasesForWorkerReturnsOnlyThatWorkersAliases() {
        WorkerAliasRepository repository = repositoryForTempDatabase();
        repository.addWorkerAlias("JANE DOE", "J. DOE");
        repository.addWorkerAlias("JANE DOE", "JANE D");
        repository.addWorkerAlias("JOHN SMITH", "J. SMITH");

        List<String> aliases = repository.getAliasesForWorker("JANE DOE");

        assertEquals(2, aliases.size());
        assertTrue(aliases.contains("J. DOE"));
        assertTrue(aliases.contains("JANE D"));
        assertFalse(aliases.contains("J. SMITH"));
    }

    @Test
    void getAliasesForWorkerIsEmptyWhenWorkerHasNoAliases() {
        WorkerAliasRepository repository = repositoryForTempDatabase();

        assertTrue(repository.getAliasesForWorker("JANE DOE").isEmpty());
    }

    @Test
    void updateWorkerAliasesReplacesExistingAliases() {
        WorkerAliasRepository repository = repositoryForTempDatabase();
        repository.addWorkerAlias("JANE DOE", "OLD ALIAS");

        String result = repository.updateWorkerAliases("JANE DOE", List.of("NEW ONE", "NEW TWO"));

        assertEquals("Worker aliases updated successfully.", result);
        List<String> aliases = repository.getAliasesForWorker("JANE DOE");
        assertEquals(2, aliases.size());
        assertTrue(aliases.contains("NEW ONE"));
        assertTrue(aliases.contains("NEW TWO"));
        assertFalse(aliases.contains("OLD ALIAS"));
    }

    @Test
    void updateWorkerAliasesWithEmptyListClearsAllAliases() {
        WorkerAliasRepository repository = repositoryForTempDatabase();
        repository.addWorkerAlias("JANE DOE", "OLD ALIAS");

        String result = repository.updateWorkerAliases("JANE DOE", List.of());

        assertEquals("Worker aliases updated successfully.", result);
        assertTrue(repository.getAliasesForWorker("JANE DOE").isEmpty());
    }

    @Test
    void updateWorkerAliasesLeavesOtherWorkersUntouched() {
        WorkerAliasRepository repository = repositoryForTempDatabase();
        repository.addWorkerAlias("JOHN SMITH", "J. SMITH");

        repository.updateWorkerAliases("JANE DOE", List.of("J. DOE"));

        assertEquals(List.of("J. SMITH"), repository.getAliasesForWorker("JOHN SMITH"));
    }

    @Test
    void deleteWorkerAliasRemovesMatchingAlias() {
        WorkerAliasRepository repository = repositoryForTempDatabase();
        repository.addWorkerAlias("JANE DOE", "J. DOE");
        repository.addWorkerAlias("JANE DOE", "JANE D");

        String result = repository.deleteWorkerAlias("JANE DOE", "J. DOE");

        assertEquals("Worker alias deleted successfully.", result);
        assertEquals(List.of("JANE D"), repository.getAliasesForWorker("JANE DOE"));
    }

    @Test
    void deleteWorkerAliasReportsFailureWhenAliasMissing() {
        WorkerAliasRepository repository = repositoryForTempDatabase();

        String result = repository.deleteWorkerAlias("JANE DOE", "MISSING");

        assertEquals("Failed to delete worker alias.", result);
    }

    @Test
    void deletingWorkerCascadeDeletesTheirAliases() {
        ConnectDB connectDB = new ConnectDB("jdbc:sqlite:" + tempDir.resolve("worker_entry.db"));
        WorkerRepository workerRepository = new WorkerRepository(connectDB);
        WorkerAliasRepository aliasRepository = new WorkerAliasRepository(connectDB);

        workerRepository.addWorker("JANE DOE");
        aliasRepository.addWorkerAlias("JANE DOE", "J. DOE");
        aliasRepository.addWorkerAlias("JANE DOE", "JANE D");

        workerRepository.deleteWorker("JANE DOE");

        assertTrue(aliasRepository.getAliasesForWorker("JANE DOE").isEmpty(),
                "aliases should be cascade-deleted when the worker is removed");
    }

    private WorkerAliasRepository repositoryForTempDatabase() {
        Path dbPath = tempDir.resolve("worker_entry.db");
        ConnectDB connectDB = new ConnectDB("jdbc:sqlite:" + dbPath);
        // Aliases reference workers via a foreign key, so the workers must exist first.
        WorkerRepository workerRepository = new WorkerRepository(connectDB);
        workerRepository.addWorker("JANE DOE");
        workerRepository.addWorker("JOHN SMITH");
        return new WorkerAliasRepository(connectDB);
    }
}
