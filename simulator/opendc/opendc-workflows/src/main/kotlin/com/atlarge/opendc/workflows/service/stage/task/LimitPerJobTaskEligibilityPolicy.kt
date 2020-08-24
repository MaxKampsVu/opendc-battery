/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.atlarge.opendc.workflows.service.stage.task

import com.atlarge.opendc.workflows.service.JobState
import com.atlarge.opendc.workflows.service.StageWorkflowSchedulerListener
import com.atlarge.opendc.workflows.service.StageWorkflowService
import com.atlarge.opendc.workflows.service.TaskState

/**
 * A [TaskEligibilityPolicy] that limits the number of active tasks of a job in the system.
 */
data class LimitPerJobTaskEligibilityPolicy(val limit: Int) : TaskEligibilityPolicy {
    override fun invoke(scheduler: StageWorkflowService): TaskEligibilityPolicy.Logic =
        object : TaskEligibilityPolicy.Logic, StageWorkflowSchedulerListener {
            private val active = mutableMapOf<JobState, Int>()

            init {
                scheduler.addListener(this)
            }

            override fun jobStarted(job: JobState) {
                active[job] = 0
            }

            override fun jobFinished(job: JobState) {
                active.remove(job)
            }

            override fun taskAssigned(task: TaskState) {
                active.merge(task.job, 1, Int::plus)
            }

            override fun taskFinished(task: TaskState) {
                active.merge(task.job, -1, Int::plus)
            }

            override fun invoke(task: TaskState): TaskEligibilityPolicy.Advice {
                val activeForJob = active[task.job]!!
                return if (activeForJob <= limit)
                    TaskEligibilityPolicy.Advice.ADMIT
                else
                    TaskEligibilityPolicy.Advice.DENY
            }
        }

    override fun toString(): String = "Limit-Active-Job($limit)"
}
