#!/bin/bash
./gradlew ktlintCheck 2>&1 | \
  grep '\.kt:' | \
  sed 's/\([^:]*\.kt:[0-9]*:[0-9]*\) \(.*\)/\1 E: \2/' | \
  reviewdog \
    -name="ktlint" \
    -reporter=github-pr-review \
    -efm="%f:%l:%c %t: %m" \
    -level=error \
    -filter-mode=nofilter \
    -fail-level=error \
    -repo="${{ github.repository }}" \
    -sha="${{ github.event.pull_request.head.sha }}" \
    -verbose