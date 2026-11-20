# Moving Out 2 — IL2CPP Class / Method Map

## Status

**PARTIALLY VERIFIED — user-supplied metadata is present locally and its header identifies IL2CPP metadata version 29. Full assembly/type/method table parsing is NOT TESTED.**

The baseline inventory did not list `global-metadata.dat`; a user has since supplied it for local analysis. The file is 15,397,704 bytes, begins with sanity magic `0xFAB11BAF`, and declares metadata version `29`. It is not committed to the repository. The matching `GameAssembly.dll` has not yet been supplied in this analysis session.

## What is blocked

Without the exact metadata file and matching binary:
- assembly, image, namespace, type, method, field, and attribute tables cannot be enumerated reliably;
- class-to-native-method RVA mapping cannot be checked;
- Unity version-specific metadata layout cannot be selected confidently;
- startup method candidates cannot be separated from dead/stripped metadata entries.

## Repository parser audit

The repository's `runtime/il2cpp/include/il2cpp_metadata.h` currently models only a short legacy header ending at `methodsCount`, comments versions 21–24, and estimates method count as `methodsCount / 32`. The parser reads that structure and checks the sanity magic only. This is **not** a complete metadata parser and is not suitable evidence for Unity 2021 LTS metadata or class/method mapping.

The parser must not be used to claim that a Moving Out 2 metadata file has been parsed. A future implementation should:
1. identify the exact metadata format/version using a documented version table;
2. validate every offset/count pair against file length with overflow-safe arithmetic;
3. parse tables using versioned structures, not guessed fixed layouts;
4. expose only non-proprietary symbol summaries needed for compatibility research;
5. reject unknown versions safely and add fixture-based tests from synthetic metadata headers.

## Required input

The user-supplied `global-metadata.dat` was analyzed locally and must not be committed, attached to a release, or redistributed. Next required input is the matching `GameAssembly.dll` (or a text-only PE/import/export report plus a locally run parser output). Keep full type/method mapping labelled NOT TESTED until parser validation and binary cross-reference are complete.
