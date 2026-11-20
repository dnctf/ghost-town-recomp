# Moving Out 2 — IL2CPP Class / Method Map

## Status

**NOT TESTED — metadata artifact not present in the baseline repository inventory.**

The baseline inventory lists `GameAssembly.dll` but does not list `global-metadata.dat`. The metadata file is often located below `<Game>_Data/il2cpp_data/Metadata/global-metadata.dat`, but that conventional path is not proof of availability for this build.

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

A local, user-provided `global-metadata.dat` can be analyzed in-session. It must not be committed, attached to a release, or redistributed. If unavailable, continue with binary strings/import analysis and label all class-map claims NOT TESTED.
