#!/usr/bin/env python3
"""Cross-file Android build/toolchain/baseline contract verifier."""
from __future__ import annotations
import re
import sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

def read(rel): return (ROOT/rel).read_text(encoding='utf-8')
errors=[]
vc=read('gradle/libs.versions.toml'); app=read('app/build.gradle.kts')
base=read('baselineprofile/build.gradle.kts'); bench=read('benchmark/build.gradle.kts')
workflow=read('.github/workflows/build.yml'); profile=read('app/src/main/baseline-prof.txt')
gen=read('baselineprofile/src/main/java/com/waheed/artificerx/baselineprofile/BaselineProfileGenerator.kt')
required=['androidCompileSdk','androidMinSdk','androidTargetSdk','androidBuildTools','androidNdk','androidCmake','agp','kotlin','ksp']
values={}
for key in required:
    m=re.search(rf'^\s*{re.escape(key)}\s*=\s*"([^"]+)"\s*$',vc,re.M)
    if not m: errors.append(f'missing version authority: {key}')
    else: values[key]=m.group(1)
for key in ['androidCompileSdk','androidMinSdk','androidTargetSdk']:
    for rel,text in [('app',app),('baselineprofile',base),('benchmark',bench)]:
        if f'libs.versions.{key}.get().toInt()' not in text:
            errors.append(f'{rel} does not consume {key}')
for rel,text in [('app',app),('baselineprofile',base),('benchmark',bench)]:
    if 'buildToolsVersion = libs.versions.androidBuildTools.get()' not in text:
        errors.append(f'{rel} does not consume androidBuildTools')
for token in ['androidNdk','androidCmake']:
    if f'libs.versions.{token}.get()' not in app: errors.append(f'app does not consume {token}')
if 'alias(libs.plugins.android.test) apply false' not in read('build.gradle.kts'): errors.append('root com.android.test registration missing')
for rel,text in [('baselineprofile',base),('benchmark',bench)]:
    if 'id("com.android.test")' not in text: errors.append(f'{rel} missing unversioned com.android.test application')
    if 'alias(libs.plugins.android.test)' in text: errors.append(f'{rel} still requests versioned android-test alias')
if 'baselineProfile(project(":baselineprofile"))' not in app: errors.append('app baselineProfile dependency missing')
if 'mergeIntoMain = true' not in app: errors.append('app baselineProfile mergeIntoMain=true missing')
if ':app:generateBaselineProfile' not in workflow: errors.append('CI baseline profile generation task missing')
if 'artificerx-generated-baseline-profile' not in workflow: errors.append('CI baseline profile artifact missing')
if 'BaselineProfileRule' not in gen or 'Canvas artwork' not in gen: errors.append('baseline generator missing real canvas journey')
if not profile.strip(): errors.append('manual baseline profile empty')
for required_rule in ['ArtificerXApp;->onCreate','MainActivity;->onCreate']:
    if required_rule not in profile: errors.append(f'manual baseline missing {required_rule}')
# CI must derive SDK/native pins from the catalog, not duplicate hardcoded authorities.
for token in ['androidCompileSdk','androidBuildTools','androidNdk','androidCmake']:
    if token not in workflow: errors.append(f'CI missing catalog-derived {token}')
# Basic HRF sanity.
rule=re.compile(r'^[HSP]*L[^;]+;(?:->[A-Za-z0-9_$<>]+\(.*\).+)?$')
for i,line in enumerate(profile.splitlines(),1):
    if not line or line.startswith('#'): continue
    if not rule.match(line.replace(' ','')): errors.append(f'baseline rule syntax suspect line {i}: {line}')
if errors:
    for e in errors: print('ERROR:',e)
    sys.exit(1)
print('BUILD CONTRACT VERIFICATION PASSED')
for k in required: print(f'{k}={values[k]}')
print('manualBaselineRules=',sum(bool(x and not x.startswith('#')) for x in profile.splitlines()))
