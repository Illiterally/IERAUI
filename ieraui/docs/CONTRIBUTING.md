# Contributing to IERAUI

Thanks for taking a look. Small, understandable improvements are especially welcome.

Please open addon bug reports in [this repository](https://github.com/Illiterally/IERAUI/issues). Include what you clicked, what you expected, what happened, and the IERAUI/Forge versions. A screenshot can help; check it for anything private before posting.

For a code change, use a branch and describe the interaction it improves. Preserve legal choices, priority, cancellation rules, and hidden information. Keep the enhancement flag's behavior intact. Prefer existing engine information over a separate approximation of the rules.

Run the checks appropriate to the behavior being changed. Add a regression test when it catches a meaningful risk, rather than tests that simply repeat the implementation. Build and package instructions are in [BUILD.md](BUILD.md).

Retain Forge's copyright/license notices. Credit assets and verify their license before adding them. Disclose substantial AI coding assistance in the pull request, following [Forge's guidance](../../CONTRIBUTING.md).

For upstream contributions, discuss the individual change with Forge's maintainers and follow their preferred workflow. This addon doesn't commit them to supporting or merging it.
