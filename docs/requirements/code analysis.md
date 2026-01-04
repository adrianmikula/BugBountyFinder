




The code analysis module will be responsible for locating bugs in the code, and determining the correct code fix for the bugs that are discovered.  

To begin with lets keep it simple and just look for the code patterns that match security bugs reported in any of the CVEs that has been issued in the last week (7 days). 

I don't want to just use plain text searches and regex matching for bugs.  We need to integrate with LLM model(s) which are adept at understanding codebase structure, code review and code fixes with a high success rate and a low halluciantion rate.  

# CVE catalog

We should group watched repos by programming languages so we can maximise reuse-ability of LLM prompts while doing our code reviews and fixes.  When a new CVE is issued, we should call the LLM to help us summarise the nature of the bug along with precise code examples of the fix in each applicable language.  Then one record per language should be inserted into our DB for each CVE, with the summary and code example for that specific language.  Only CVEs which meet our configured severity threshhold should be indexed, as we will probably skip low-risk CVEs which might not receive a high enough bounty to be worth tracking.

# Codebase index

However, the tricky part is to also add some broader codebase context and understanding, so that various classes, methods and package structures are meaningful inside the LLM context, giving it a broader understanding of the commit diff and how it fits into the wider codebase.  In order to achieve this level of codebase understanding, let's research an effective way to create a lightweight index of the whole codebase for repos of various languages in 2026, and store the index inside a DB.

# Commit reviews

Whenever a commit is added to a watched repo, we can quickly query for CVEs which are relevant to the languages used in the repo, and easily build a single LLM prompt which can locate any of the CVEs if they are present in the commit diff. The prompt will also have the codebasee index included in it, so the LLM is aware of the specific codebase structure, rather than relying on training data patterns which is likely to produce hallucinations. The LLM model should be asked to determine if the new commit contains any of the described CVEs, and list which ones are present in the new code. 

# Individual CVE prompt

If our initial analysis confirms that one or more CVEs are present, then we will then loop through each CVE, and build a more focused LLM prompt for each CVE.  The focused prompt will include the CVE in the current loop, along with the codebase index and the commit diff. We also want to add to this prompt, the full text for each source file touched by the commit, so that the LLM has a more complete reference of all the code related to this commit. This will involve pulling individual files from the repo.

# Bug verification and fix

With this enhanced prompt, we will ask a different LLM to double-check the presence of each CVE in the commit, and rank it's confidence in the presence of the CVE. So we have cross-confirmation by different coding models.  If the second LLM confirms that the CVE is present, then we will call it again and ask for a recommended code fix. Finally, once we get a recommended code fix, we will pass the recommended code fix along with the other prompt details back to the first LLM for a final confirmation cross-check of the fix and a general code review.  We will also ask the final LLM model to rate its confidence in the correctness of the fix.  

# Human intervention

If the CVE presence or fix cross-checks receive a 'low-confidence' or 'unsure' response, then we will insert a DB record with the possible bug into our DB, and queue it to receive human review.  Once confirmed, the code will just continue along the normal code path. We will later build a web UI to let the human reveiw the potential bug/fix and either confirm or reject it.  We will also later add Slack notifications so the human can be instantly notified of the potential bugbounty discovery.  

# PR creation

Once both the presence of the CVE and the fix for the CVE are double-confirmed, then we will update our DB record to indicate that this is a confirmed high-value bugbounty fix.  Then we will use the Github API to create a branch and a PR for the fix.  We will also need to manage things like ensuring that we have access to the project, writing up a meaningful commit comment, and doing any other steps required to claim the bugbounty rewards for this repo such as sending an email to the repo owners outlining the CVE and the suggested fix. 


