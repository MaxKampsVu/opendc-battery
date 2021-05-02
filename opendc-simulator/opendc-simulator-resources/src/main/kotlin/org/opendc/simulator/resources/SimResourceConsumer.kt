/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.simulator.resources

/**
 * A [SimResourceConsumer] characterizes how a resource is consumed.
 *
 * Implementors of this interface should be considered stateful and must be assumed not to be re-usable (concurrently)
 * for multiple resource providers, unless explicitly said otherwise.
 */
public interface SimResourceConsumer {
    /**
     * This method is invoked when the consumer is started for some resource.
     *
     * @param ctx The execution context in which the consumer runs.
     */
    public fun onStart(ctx: SimResourceContext) {}

    /**
     * This method is invoked when a resource asks for the next [command][SimResourceCommand] to process, either because
     * the resource finished processing, reached its deadline or was interrupted.
     *
     * @param ctx The execution context in which the consumer runs.
     * @return The next command that the resource should execute.
     */
    public fun onNext(ctx: SimResourceContext): SimResourceCommand

    /**
     * This method is invoked when the resource provider confirms that the consumer is running at the given speed.
     *
     * @param ctx The execution context in which the consumer runs.
     * @param speed The speed at which the consumer runs.
     */
    public fun onConfirm(ctx: SimResourceContext, speed: Double) {}

    /**
     * This is method is invoked when the capacity of the resource changes.
     *
     * After being informed of such an event, the consumer might decide to adjust its consumption by interrupting the
     * resource via [SimResourceContext.interrupt]. Alternatively, the consumer may decide to ignore the event, possibly
     * causing the active resource command to finish at a later moment than initially planned.
     *
     * @param ctx The execution context in which the consumer runs.
     * @param isThrottled A flag to indicate that the active resource command will be throttled as a result of the
     * capacity change.
     */
    public fun onCapacityChanged(ctx: SimResourceContext, isThrottled: Boolean) {}

    /**
     * This method is invoked when the consumer has finished, either because it exited via [SimResourceCommand.Exit],
     * the resource finished itself, or a failure occurred at the resource.
     *
     * Note that throwing an exception in [onStart] or [onNext] is undefined behavior and up to the resource provider.
     *
     * @param ctx The execution context in which the consumer ran.
     * @param cause The cause of the finish in case the resource finished exceptionally.
     */
    public fun onFinish(ctx: SimResourceContext, cause: Throwable? = null) {}
}