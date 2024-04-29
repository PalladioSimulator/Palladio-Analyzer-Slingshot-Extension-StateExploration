# Palladio-Analyzer-Slingshot-Extension-StateExploration

Slingshot Extension to do state space exploration as envisioned for the MENTOR DFG project.  

Still under construction. 

At first, follow the steps in the normal Slingshot Documentation (https://palladiosimulator.github.io/Palladio-Documentation-Slingshot/tutorial/) to setup Slingshot and maybe run an (normal) example. 
Pay attention to also import the SPD meta model and the SPD interpreter extension.

Now, for the State Exploration:
* clone StateExploration Repository from GitHub: 
  - https://github.com/PalladioSimulator/Palladio-Analyzer-Slingshot-Extension-StateExploration
* in (already cloned) repository *Palladio-Analyzer-Slingshot* switch to branch `stateexplorationRequirements`
* in (already cloned) repository *Palladio-Analyzer-Slingshot-Extension-PCM-Core* switch to branch `stateexplorationRequirements`
  - beware : requires `...spd.semantic` from the repository *Palladio-Addons-SPD-Metamodel*, please import into workspace as well.
* all other Slingshot repositories may remain on `master`. If any of the repositories on master cause problems, please report.    


- some of these state exploration requirements are temporary workarounds, others i'll try to get into the actual slingshot :)


## Run Headless State-Exploration
+ Execute all the steps above, as a result you should be able to execute the State Exploration from inside you Runtime Eclipse instance. 
+ Import Experiment-Automation bundles. 
  * clone repository [Palladio-Addons-ExperimentAutomation](https://github.com/PalladioSimulator/Palladio-Addons-ExperimentAutomation) and switch to branch `slingshot-impl`:
  ```
  git clone git@github.com:PalladioSimulator/Palladio-Addons-ExperimentAutomation.git
  git checkout slingshot-impl
  ```
  * import these bundles into the workspace of your development Eclipse instance.
    * `org.palladiosimulator.experimentautomation`
    * `org.palladiosimulator.experimentautomation.edit`
    * `org.palladiosimulator.experimentautomation.editor`
    + note: we only need the models, thus we only need these three bundles. 

+ Start a Runtime Eclipse instance and create a `*.experiments` model, remember the absolute path to that model and close the Runtime instance.
  * Or use the this one: https://git.rss.iste.uni-stuttgart.de/slingshot/examples/mentorexample/-/tree/ts-scenarion-for-headless.

+ Create an *OSGi Framework* Run Configuration.
  * Or use this one `org.palladiosimulator.analyzer.slingshot.stateexploration.application/launchconfig/headless-exploration-export.launch` 
    * **Beware:** you still need to fix the *Program arguments* (see below)
  * Exclude all `*.edit`, `*.editor` and `*.ui` bundles. 
    * Excluding `*.edit` is not required, but we don't need it.
    * Only exclude `*.ui`. The `*.ui.events` are still needed.   
  * Uncheck *Include optional dependencies [...]* 
  * Click *Add Required Bundles*, to add dependencies
  * Click *Validate Bundles*, just to be on the safe side.
  * Go to tab *Arguments*
  * Add to *Program arguments*: `-application org.palladiosimulator.analyzer.slingshot.stateexploration.application.StateexplorationApplication [/absolute/path/to/experiments/file.experiments]`
  * Remove from *VM arguments*: `-Declipse.ignoreApp=true`
  * Run it.


