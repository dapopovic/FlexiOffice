./gradlew lintKotlin 2>&1 | grep 'Lint error > ' | sed 's/ Lint error > /e: /g' | reviewdog  \
-name="ktlint" \
-reporter=github-pr-review \
-efm="%f:%l:%c:%t: %m" \
-level=error \
-filter-mode=nofilter \
-fail-level=error \