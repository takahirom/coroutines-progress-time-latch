Coroutines progress time latch

# How to use

```kotlin
  lateinit var progressTimeLatch: CoroutinesProgressTimeLatch
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    progressTimeLatch = CoroutinesProgressTimeLatch(viewLifecycleOwner.lifecycleScope) { isVisible ->
      binding.progress.isVisible = isVisible
    }
    viewModel.isProgress.observe(viewLifecycleOwner) { isProgress ->
      progressTimeLatch.refresh(value == isProgress)
    }
```


# Dependencies
Add it in your root build.gradle at the end of repositories:

```groovy
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```


Step 2. Add the dependency

```groovy
	dependencies {
	        implementation 'com.github.takahirom:coroutines-progress-time-latch:Tag'
	}
```
